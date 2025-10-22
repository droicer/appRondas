package com.ronda.rondacajamarcaapp.auth

data class User(
    val uid: String = "",
    val name: String = "",
    val dni: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "USER" // ✅ rol por defecto
)
