// File: app/src/main/java/com/ronda/rondacajamarcaapp/auth/EmergencyViewModel.kt
package com.ronda.rondacajamarcaapp.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder

class EmergencyViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // LISTA DE EMERGENCIAS (solo activas en AdminScreen)
    private val _emergencies = MutableStateFlow<List<Emergency>>(emptyList())
    val emergencies: StateFlow<List<Emergency>> = _emergencies

    init {
        listenEmergencies()
    }

    /**
     * ESCUCHA TODAS LAS EMERGENCIAS EN TIEMPO REAL
     */
    fun listenEmergencies() {
        db.collection("emergencies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EmergencyVM", "Error escuchando emergencias", error)
                    _emergencies.value = emptyList()
                    return@addSnapshotListener
                }

                val list = snapshot?.toObjects(Emergency::class.java) ?: emptyList()
                _emergencies.value = list
                Log.d("EmergencyVM", "Emergencias actualizadas: ${list.size}")
            }
    }

    /**
     * CREAR EMERGENCIA + NOTIFICAR A ADMINS
     */
    fun createEmergency(userId: String, userName: String, lat: Double, lng: Double) {
        val data = hashMapOf(
            "id" to "",
            "createdBy" to userId,
            "createdByName" to userName,
            "location" to GeoPoint(lat, lng),
            "status" to "activa",
            "timestamp" to FieldValue.serverTimestamp()
        )

        viewModelScope.launch {
            try {
                val docRef = db.collection("emergencies").add(data).await()
                docRef.update("id", docRef.id).await()

                // NOTIFICAR A ADMINS
                notifyAdmins("$userName reportó una nueva emergencia")

                Log.d("EMERGENCY", "Emergencia creada con ID: ${docRef.id}")
            } catch (e: Exception) {
                Log.e("EMERGENCY", "Error creando emergencia", e)
            }
        }
    }

    /**
     * CERRAR EMERGENCIA (solo Admin)
     */
    fun closeEmergency(id: String) {
        Log.d("EMERGENCY", "Cerrando emergencia: $id")
        viewModelScope.launch {
            try {
                db.collection("emergencies").document(id)
                    .update("status", "cerrada")
                    .await()
                Log.d("EMERGENCY", "Emergencia $id cerrada")
            } catch (e: Exception) {
                Log.e("EMERGENCY", "Error cerrando emergencia $id", e)
            }
        }
    }

    /**
     * ENVÍA NOTIFICACIÓN A ADMINS CON type=emergencia
     */
    private fun notifyAdmins(message: String) {
        val url = "https://droicer.com/rondas/sendNotification.php"

        // Codificar mensaje para evitar errores con caracteres especiales
        val bodyEncoded = URLEncoder.encode(message, "UTF-8")
        val postData = "body=$bodyEncoded&type=emergencia"

        Thread {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))

                val response = conn.inputStream.bufferedReader().readText()
                Log.d("FCM_EMERGENCY", "Notificación EMERGENCIA enviada: $response")
            } catch (e: Exception) {
                Log.e("FCM_EMERGENCY", "Error enviando notificación de emergencia", e)
            }
        }.start()
    }
}