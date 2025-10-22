package com.ronda.rondacajamarcaapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReportViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Reportes generales (para admin)
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    // Reportes del usuario logueado (para rondero/usuario)
    private val _userReports = MutableStateFlow<List<Report>>(emptyList())
    val userReports: StateFlow<List<Report>> = _userReports

    /** Escuchar en tiempo real todos los reportes (para ADMIN) */
    fun listenReports() {
        db.collection("reports").addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { d ->
                d.toObject(Report::class.java)?.copy(id = d.id)
            } ?: emptyList()
            viewModelScope.launch { _reports.value = list }
        }
    }

    /** Escuchar en tiempo real los reportes de un usuario específico */
    fun listenUserReports(userId: String) {
        db.collection("reports").whereEqualTo("createdBy", userId)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { d ->
                    d.toObject(Report::class.java)?.copy(id = d.id)
                } ?: emptyList()
                viewModelScope.launch { _userReports.value = list }
            }
    }

    /** Crear un reporte nuevo */
    suspend fun createReport(report: Report) {
        db.collection("reports").add(report).await()
    }

    /** Cambiar estado del reporte */
    fun updateReportStatus(id: String, newStatus: String) {
        viewModelScope.launch {
            try {
                db.collection("reports").document(id).update("status", newStatus).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
