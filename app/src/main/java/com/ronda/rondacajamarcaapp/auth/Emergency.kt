package com.ronda.rondacajamarcaapp.auth

import com.google.firebase.firestore.GeoPoint

data class Emergency(
    var id: String = "",
    var createdBy: String = "",
    var createdByName: String = "",   // 👈 Nombre del usuario
    var location: GeoPoint? = null    // 👈 Coordenadas
)
