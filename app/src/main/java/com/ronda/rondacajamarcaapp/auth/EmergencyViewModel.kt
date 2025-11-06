package com.ronda.rondacajamarcaapp.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EmergencyViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _emergencies = MutableStateFlow<List<Emergency>>(emptyList())
    val emergencies: StateFlow<List<Emergency>> = _emergencies

    fun createEmergency(userId: String, userName: String, lat: Double, lng: Double) {
        val data = hashMapOf(
            "id" to "",
            "createdBy" to userId,
            "createdByName" to userName,
            "location" to com.google.firebase.firestore.GeoPoint(lat, lng),
            "status" to "activa",
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("emergencies")
            .add(data)
            .addOnSuccessListener { doc ->
                doc.update("id", doc.id)
            }
    }

    fun listenEmergencies() {
        db.collection("emergencies")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(Emergency::class.java) ?: emptyList()
                _emergencies.value = list
            }
    }

    fun closeEmergency(id: String) {
        db.collection("emergencies").document(id)
            .update("status", "cerrada")
    }
}