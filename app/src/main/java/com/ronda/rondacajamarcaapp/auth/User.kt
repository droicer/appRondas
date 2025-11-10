// File: app/src/main/java/com/ronda/rondacajamarcaapp/auth/User.kt
package com.ronda.rondacajamarcaapp.auth

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val dni: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "USER",

    // ELIMINA `password` (nunca en Firestore)
    // AÑADE `fcmToken` para notificaciones
    @PropertyName("fcmToken")
    @get:PropertyName("fcmToken")
    @set:PropertyName("fcmToken")
    var fcmToken: String? = null
)