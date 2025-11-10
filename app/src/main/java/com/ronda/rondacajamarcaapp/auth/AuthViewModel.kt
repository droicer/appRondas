// File: app/src/main/java/com/ronda/rondacajamarcaapp/auth/AuthViewModel.kt
package com.ronda.rondacajamarcaapp.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // LISTA DE USUARIOS
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    // USUARIO ACTUAL
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // ESTADO DE CARGA
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                db.collection("users").document(uid).addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.e("AUTH_VM", "Error escuchando usuario", error)
                        _currentUser.value = null
                        _isLoading.value = false
                        return@addSnapshotListener
                    }

                    val userData = snap?.toObject(User::class.java)?.copy(uid = uid)
                    _currentUser.value = userData
                    _isLoading.value = false
                    Log.d("AUTH_VM", "Usuario cargado: ${userData?.name}")
                }
            } else {
                _currentUser.value = null
                _isLoading.value = false
                Log.d("AUTH_VM", "No hay usuario autenticado")
            }
        }
    }

    // GUARDAR FCM TOKEN
    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("FCM_TOKEN", "Error obteniendo token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val currentUser = Firebase.auth.currentUser

            if (currentUser != null) {
                Log.d("FCM_TOKEN", "Token obtenido: $token")
                Firebase.firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM_TOKEN", "Token GUARDADO en Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM_TOKEN", "Error guardando token", e)
                    }
            } else {
                Log.w("FCM_TOKEN", "No hay usuario autenticado")
            }
        }
    }

    /** REGISTRO */
    suspend fun register(email: String, password: String, user: User): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return false

            val userData = user.copy(uid = uid)
            db.collection("users").document(uid).set(userData).await()

            registerFcmToken()
            true
        } catch (e: Exception) {
            Log.d("AUTH_VM", "Error en registro", e)
            false
        }
    }

    /** LOGIN + SUSCRIPCIÓN A TÓPICOS */
    suspend fun login(email: String, password: String): User? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return null

            registerFcmToken()

            val snap = db.collection("users").document(uid).get().await()
            val userData = snap.toObject(User::class.java)?.copy(uid = uid)

            // SUSCRIBIR A 'admins' SI ES ADMIN
            if (userData?.role?.equals("ADMIN", ignoreCase = true) == true) {
                Log.d("FCM_TOPIC", "Suscribiendo a 'admins'...")
                FirebaseMessaging.getInstance().subscribeToTopic("admins")
                    .addOnSuccessListener { Log.d("FCM_TOPIC", "SUSCRITO a 'admins'") }
                    .addOnFailureListener { e -> Log.e("FCM_TOPIC", "Error suscribiendo", e) }
            }

            userData
        } catch (e: Exception) {
            Log.e("AUTH_VM", "Error en login", e)
            null
        }
    }

    /** ESCUCHA TODOS LOS USUARIOS */
    fun listenUsers() {
        db.collection("users").addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("AUTH_VM", "Error escuchando usuarios", error)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                val u = doc.toObject(User::class.java) ?: return@mapNotNull null
                if (u.uid.isBlank()) u.copy(uid = doc.id) else u
            } ?: emptyList()
            viewModelScope.launch { _users.emit(list) }
        }
    }

    fun updateUserRole(uid: String, newRole: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).update("role", newRole.uppercase()).await()
            } catch (e: Exception) {
                Log.e("AUTH_VM", "Error actualizando rol", e)
            }
        }
    }

    /**
     * CERRAR SESIÓN + DESUSCRIBIRSE DE 'admins' SI ERA ADMIN
     */
    fun logout() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Verificar si era admin
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role")
                    if (role?.equals("ADMIN", ignoreCase = true) == true) {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("admins")
                            .addOnSuccessListener {
                                Log.d("FCM_TOPIC", "Admin DESUSCRITO del topic 'admins'")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FCM_TOPIC", "Error al desuscribir", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOPIC", "Error verificando rol al logout", e)
                }
        }

        // Cerrar sesión
        auth.signOut()
        Log.d("AUTH_VM", "Sesión cerrada")
    }
}