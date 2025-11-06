package com.ronda.rondacajamarcaapp.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReportViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // LISTA GLOBAL (para Admin)
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    // LISTA POR USUARIO (para Rondero y User)
    private val _userReports = MutableStateFlow<List<Report>>(emptyList())
    val userReports: StateFlow<List<Report>> = _userReports

    fun listenReports() {
        db.collection("reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ReportVM", "Error escuchando reportes", error)
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
                    android.util.Log.e("ReportVM", "Error en mis reportes", error)
                    _userReports.value = emptyList()
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Report::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _userReports.value = list
            }
    }


    // CREAR REPORTE (CON ID CORRECTO)
    fun createReport(report: Report) {
        val docRef = db.collection("reports").document()  // Genera ID
        val newReport = report.copy(id = docRef.id)       // Asigna ID

        docRef.set(newReport.toMap())
            .addOnSuccessListener {
                android.util.Log.d("ReportVM", "Reporte creado con ID: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ReportVM", "Error al crear reporte", e)
            }
    }

    // CAMBIAR ESTADO (Admin)
    fun updateReportStatus(id: String, newStatus: String) {
        db.collection("reports").document(id)
            .update("status", newStatus)
            .addOnSuccessListener {
                android.util.Log.d("ReportVM", "Estado actualizado: $newStatus")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ReportVM", "Error al actualizar estado", e)
            }
    }
}