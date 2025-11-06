package com.ronda.rondacajamarcaapp.auth

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Emergency(
    var id: String = "",
    var createdBy: String = "",
    var createdByName: String = "",
    var location: GeoPoint? = null,
    var timestamp: Timestamp? = null,   // Acepta Timestamp o null
    var status: String = "activa"
)