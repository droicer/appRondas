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

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                db.collection("users").document(uid).addSnapshotListener { snap, _ ->
                    val u = snap?.toObject(User::class.java)?.copy(uid = uid)
                    _currentUser.value = u
                }
            } else {
                _currentUser.value = null
            }
        }
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result
            val currentUser = Firebase.auth.currentUser

            if (currentUser != null) {
                Firebase.firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token guardado para UID: ${currentUser.uid}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Error al guardar token", e)
                    }
            }
        }
    }

    /** Registro con Auth + Firestore */
    suspend fun register(email: String, password: String, user: User): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return false

            val userData = user.copy(uid = uid)
            db.collection("users").document(uid).set(userData).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Login: devuelve el User guardado en Firestore */
    suspend fun login(email: String, password: String): User? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return null
            val snap = db.collection("users").document(uid).get().await()
            snap.toObject(User::class.java)?.copy(uid = uid)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Escucha en tiempo real la colección de usuarios */
    fun listenUsers() {
        db.collection("users").addSnapshotListener { snap, _ ->
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
                db.collection("users").document(uid).update("role", newRole).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout() = auth.signOut()
}
