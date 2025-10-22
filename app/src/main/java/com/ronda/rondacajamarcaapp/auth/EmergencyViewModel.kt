package com.ronda.rondacajamarcaapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmergencyViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _emergencies = MutableStateFlow<List<Emergency>>(emptyList())
    val emergencies: StateFlow<List<Emergency>> = _emergencies

    private var listener: ListenerRegistration? = null

    /** Escucha emergencias en tiempo real */
    fun listenEmergencies() {
        listener?.remove()
        listener = db.collection("emergencies")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { d ->
                    d.toObject(Emergency::class.java)?.copy(id = d.id)
                } ?: emptyList()
                viewModelScope.launch { _emergencies.value = list }
            }
    }

    /** Crear emergencia con UID + nombre + ubicación */
    fun createEmergency(userId: String, userName: String, lat: Double, lng: Double) {
        val e = Emergency(
            createdBy = userId,
            createdByName = userName,
            location = GeoPoint(lat, lng)
        )
        db.collection("emergencies").add(e)
    }

    /** Cerrar emergencia (borrar documento) */
    fun closeEmergency(id: String) {
        db.collection("emergencies").document(id).delete()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
