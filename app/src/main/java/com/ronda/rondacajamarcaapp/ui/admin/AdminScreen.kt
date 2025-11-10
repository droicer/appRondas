// File: app/src/main/java/com/ronda/rondacajamarcaapp/ui/admin/AdminScreen.kt
package com.ronda.rondacajamarcaapp.ui.admin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ronda.rondacajamarcaapp.auth.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    user: User,
    reportVM: ReportViewModel,
    emergencyVM: EmergencyViewModel,
    authVM: AuthViewModel,
    onLogout: () -> Unit
) {
    val reports by reportVM.reports.collectAsState()
    val emergencies by emergencyVM.emergencies.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    // FILTRAR SOLO EMERGENCIAS ACTIVAS
    val activeEmergencies = emergencies.filter { it.status == "activa" }

    LaunchedEffect(Unit) {
        reportVM.listenReports()
        emergencyVM.listenEmergencies()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel de Administración", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(user.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { authVM.logout(); onLogout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.List, null)
                        Text("Reportes (${reports.size})")
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(containerColor = if (activeEmergencies.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant) {
                            Icon(Icons.Default.Warning, null)
                        }
                        Text("Emergencias (${activeEmergencies.size})")
                    }
                }
            }

            when (selectedTab) {
                0 -> ReportsContent(reports = reports, reportVM = reportVM)
                1 -> EmergenciesContent(emergencies = activeEmergencies, emergencyVM = emergencyVM)
            }
        }
    }
}

// ==================== REPORTES ====================
@Composable
private fun ReportsContent(reports: List<Report>, reportVM: ReportViewModel) {
    if (reports.isEmpty()) {
        EmptyState(Icons.Default.CheckCircle, "No hay reportes", "Todos los reportes están resueltos")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reports, key = { it.id }) { report ->
                ReportCard(
                    report = report,
                    onStatusChange = { reportVM.updateReportStatus(report.id, it) },
                    onDelete = { reportVM.deleteReport(report.id) } // FUNCIONA
                )
            }
        }
    }
}

@Composable
private fun ReportCard(report: Report, onStatusChange: (String) -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val statusColor = when (report.status) {
        "pendiente" -> MaterialTheme.colorScheme.error
        "atendido" -> MaterialTheme.colorScheme.tertiary
        "resuelto" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = { },
                    label = { Text(report.status.uppercase()) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = statusColor.copy(alpha = 0.15f), labelColor = statusColor),
                    leadingIcon = {
                        Icon(
                            when (report.status) {
                                "pendiente" -> Icons.Default.Schedule
                                "atendido" -> Icons.Default.Construction
                                "resuelto" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Info
                            }, null, modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(report.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(report.createdByName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // FOTO EN BASE64
            report.photoBase64?.let { base64 ->
                Spacer(Modifier.height(12.dp))
                val bitmap = remember(base64) {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Foto del reporte",
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botón expandir
            FilledTonalButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                Spacer(Modifier.width(8.dp))
                Text(if (expanded) "Ocultar Detalles" else "Ver en Mapa")
            }

            // MAPA + ESTADO + BORRAR
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    // MAPA
                    report.location?.let { geoPoint ->
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                        GoogleMapView(latLng = latLng, title = report.title)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Cambiar estado
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cambiar Estado")
                            Spacer(Modifier.weight(1f))
                            Icon(if (dropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }

                        DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            listOf("pendiente" to Icons.Default.Schedule, "atendido" to Icons.Default.Construction, "resuelto" to Icons.Default.CheckCircle)
                                .forEach { (status, icon) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Icon(icon, null)
                                                Text(status.replaceFirstChar { it.uppercase() })
                                            }
                                        },
                                        onClick = {
                                            onStatusChange(status)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                        }
                    }

                    // BOTÓN BORRAR (solo si resuelto)
                    if (report.status == "resuelto") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Borrar Reporte")
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO DE BORRADO
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Borrar reporte") },
            text = { Text("¿Estás seguro? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// --- MAPA CON GOOGLE MAPS ---
@Composable
fun GoogleMapView(latLng: LatLng, title: String) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    AndroidView(
        factory = {
            mapView.apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300)
                onCreate(null)
                onResume()
            }
        },
        update = { view ->
            view.getMapAsync { googleMap ->
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(latLng).title(title))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                googleMap.uiSettings.isZoomControlsEnabled = true
            }
        }
    )
}

// ==================== EMERGENCIAS ====================
@Composable
private fun EmergenciesContent(emergencies: List<Emergency>, emergencyVM: EmergencyViewModel) {
    if (emergencies.isEmpty()) {
        EmptyState(Icons.Default.Check, "Sin emergencias", "Todo en calma")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(emergencies, key = { it.id }) { emergency ->
                EmergencyCard(emergency = emergency, onClose = { emergencyVM.closeEmergency(emergency.id) })
            }
        }
    }
}

@Composable
private fun EmergencyCard(emergency: Emergency, onClose: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("EMERGENCIA ACTIVA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("ID: ${emergency.id.take(8)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                }
            }

            Spacer(Modifier.height(16.dp))

            ListItem(headlineContent = { Text("Usuario") }, supportingContent = { Text(emergency.createdByName) }, leadingContent = { Icon(Icons.Default.Person, null) })

            emergency.location?.let { gp ->
                Spacer(Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("Ubicación") },
                    supportingContent = { Text("${String.format("%.6f", gp.latitude)}, ${String.format("%.6f", gp.longitude)}") },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) }
                )
                Spacer(Modifier.height(12.dp))
                GoogleMapView(latLng = LatLng(gp.latitude, gp.longitude), title = "Emergencia")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar Emergencia", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Cerrar emergencia?") },
            text = { Text("¿Estás seguro?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClose()
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sí") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, description: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}