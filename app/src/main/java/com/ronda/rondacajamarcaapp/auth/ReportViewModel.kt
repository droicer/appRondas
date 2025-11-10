// File: app/src/main/java/com/ronda/rondacajamarcaapp/auth/ReportViewModel.kt
package com.ronda.rondacajamarcaapp.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URLEncoder

class ReportViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    private val _userReports = MutableStateFlow<List<Report>>(emptyList())
    val userReports: StateFlow<List<Report>> = _userReports

    init { listenReports() }

    fun listenReports() {
        db.collection("reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReportVM", "Error escuchando reportes", error)
                    _reports.value = emptyList()
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Report::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _reports.value = list
            }
    }

    fun listenUserReports(userId: String) {
        db.collection("reports")
            .whereEqualTo("createdBy", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _userReports.value = emptyList()
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Report::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _userReports.value = list
            }
    }

    // CREAR REPORTE (CON BASE64)
    fun createReport(
        report: Report,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val docRef = db.collection("reports").document()
        val newReport = report.copy(id = docRef.id)

        // GUARDAR EN FIRESTORE
        docRef.set(newReport.toMap())
            .addOnSuccessListener {
                Log.d("ReportVM", "Reporte creado con ID: ${docRef.id}")
                // NOTIFICAR SOLO DESPUÉS DE GUARDAR
                notifyAdmins("${newReport.createdByName} creó un nuevo reporte: ${newReport.title}")
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("ReportVM", "Error al crear reporte", e)
                onComplete(false, e.message)
            }
    }

    // ACTUALIZAR ESTADO
    fun updateReportStatus(id: String, newStatus: String) {
        db.collection("reports").document(id)
            .update("status", newStatus)
            .addOnSuccessListener {
                Log.d("ReportVM", "Estado actualizado a: $newStatus")
            }
            .addOnFailureListener { e ->
                Log.e("ReportVM", "Error actualizando estado", e)
            }
    }

    // BORRAR REPORTE
    fun deleteReport(id: String) {
        Log.d("ReportVM", "Borrando reporte: $id")
        db.collection("reports").document(id)
            .delete()
            .addOnSuccessListener {
                Log.d("ReportVM", "Reporte $id BORRADO")
            }
            .addOnFailureListener { e ->
                Log.e("ReportVM", "Error borrando reporte", e)
            }
    }

    /**
     * NOTIFICACIÓN SOLO DESPUÉS DE GUARDAR EN FIRESTORE
     */
    private fun notifyAdmins(message: String) {
        val url = "https://droicer.com/rondas/sendNotification.php"
        val bodyEncoded = URLEncoder.encode(message, "UTF-8")
        val postData = "body=$bodyEncoded&type=reporte"

        Thread {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.connectTimeout = 15000  // 15 segundos
                conn.readTimeout = 15000
                conn.outputStream.write(postData.toByteArray(Charsets.UTF_8))

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                }

                Log.d("FCM_REPORT", "Notificación enviada: $response (Código: $responseCode)")
            } catch (e: Exception) {
                Log.e("FCM_REPORT", "Error enviando notificación", e)
            }
        }.start()
    }
}