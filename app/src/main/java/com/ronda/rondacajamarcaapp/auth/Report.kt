package com.ronda.rondacajamarcaapp.auth

import com.google.firebase.firestore.GeoPoint

data class Report(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var status: String = "pendiente",
    var createdBy: String = "",
    var createdByName: String = "",
    var location: GeoPoint? = null,
    val isEmergency: Boolean = false
)
