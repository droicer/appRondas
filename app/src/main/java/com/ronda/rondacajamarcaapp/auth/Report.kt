package com.ronda.rondacajamarcaapp.auth

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

data class Report(
    @PropertyName("id") var id: String = "",
    @PropertyName("title") var title: String = "",
    @PropertyName("description") var description: String = "",
    @PropertyName("status") var status: String = "pendiente",
    @PropertyName("createdBy") var createdBy: String = "",
    @PropertyName("createdByName") var createdByName: String = "",
    @PropertyName("location") var location: GeoPoint? = null,
    @PropertyName("photoBase64") var photoBase64: String? = null,
    @ServerTimestamp var timestamp: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "status" to status,
        "createdBy" to createdBy,
        "createdByName" to createdByName,
        "location" to location,
        "photoBase64" to photoBase64,
        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}