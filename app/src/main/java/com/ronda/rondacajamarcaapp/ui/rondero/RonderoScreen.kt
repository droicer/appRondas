package com.ronda.rondacajamarcaapp.ui.rondero

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.firestore.GeoPoint
import com.ronda.rondacajamarcaapp.auth.*
import com.ronda.rondacajamarcaapp.utils.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RonderoScreen(
    user: User,
    reportVM: ReportViewModel,
    emergencyVM: EmergencyViewModel,
    logout: () -> Unit
) {
    val reports by reportVM.userReports.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        reportVM.listenUserReports(user.uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel de Rondero", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(user.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = logout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                // ← AQUÍ ESTÁ LA CORRECCIÓN
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showEmergencyDialog = true },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Emergencia", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Add, null)
                            Text("Crear Reporte")
                        }
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.List, null)
                            Text("Mis Reportes (${reports.size})")
                        }
                    }
                }

                when (selectedTab) {
                    0 -> CreateReportTab(user, reportVM) {
                        successMessage = "Reporte creado"
                        showSuccessSnackbar = true
                        selectedTab = 1
                    }
                    1 -> MyReportsTab(reports)
                }
            }

            if (showSuccessSnackbar) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(3000); showSuccessSnackbar = false }
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Text(successMessage)
                    }
                }
            }
        }
    }

    // Diálogo emergencia
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)) },
            title = { Text("Activar Emergencia") },
            text = { Text("¿Notificar a administradores?") },
            confirmButton = {
                Button(
                    onClick = {
                        LocationUtils.getCurrentLocation(context) { lat, lng ->
                            emergencyVM.createEmergency(user.uid, user.name, lat, lng)
                            successMessage = "Emergencia activada"
                            showSuccessSnackbar = true
                        }
                        showEmergencyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sí") }
            },
            dismissButton = { TextButton(onClick = { showEmergencyDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CreateReportTab(
    user: User,
    reportVM: ReportViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDenied by remember { mutableStateOf(false) }

    val photoFile = remember { createTempImageFile(context) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoUri = photoFile.toUri(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(photoFile.toUri(context))
        else showPermissionDenied = true
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            OutlinedCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nuevo Reporte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Título") }, leadingIcon = { Icon(Icons.Default.Title, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !isLoading
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Descripción") }, leadingIcon = { Icon(Icons.Default.Description, null) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), minLines = 4, enabled = !isLoading
                    )

                    Spacer(Modifier.height(16.dp))

                    // BOTÓN CÁMARA
                    if (hasCamera) {
                        Button(
                            onClick = {
                                when {
                                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                                            PackageManager.PERMISSION_GRANTED -> {
                                        cameraLauncher.launch(photoFile.toUri(context))
                                    }
                                    else -> permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tomar Foto")
                        }
                    } else {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text("Cámara no disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // VISTA PREVIA
                    photoUri?.let { uri ->
                        Spacer(Modifier.height(12.dp))
                        AsyncImage(
                            model = uri, contentDescription = "Foto",
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        TextButton(onClick = { photoUri = null }, modifier = Modifier.align(Alignment.End)) {
                            Text("Eliminar")
                        }
                    }

                    ListItem(
                        headlineContent = { Text("Reportado por") },
                        supportingContent = { Text(user.name) },
                        leadingContent = { Icon(Icons.Default.Person, null) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    isLoading = true
                    LocationUtils.getCurrentLocation(context) { lat, lng ->
                        scope.launch {
                            try {
                                val photoBase64 = photoUri?.let { uri -> uriToBase64(uri, context) }

                                val report = Report(
                                    title = title.trim(),
                                    description = description.trim(),
                                    status = "pendiente",
                                    createdBy = user.uid,
                                    createdByName = user.name,
                                    location = GeoPoint(lat, lng),
                                    photoBase64 = photoBase64
                                )
                                reportVM.createReport(report)
                                title = ""; description = ""; photoUri = null; isLoading = false
                                onSuccess()
                            } catch (e: Exception) {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = title.isNotBlank() && description.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Creando...")
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Reporte")
                }
            }
        }
    }

    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Permiso requerido") },
            text = { Text("Necesitas la cámara para tomar fotos.") },
            confirmButton = { TextButton(onClick = { showPermissionDenied = false }) { Text("OK") } }
        )
    }
}

// --- BASE64 CONVERSIÓN ---
private suspend fun uriToBase64(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri)!!
    val bitmap = BitmapFactory.decodeStream(inputStream)
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos) // < 300KB
    val bytes = baos.toByteArray()
    Base64.encodeToString(bytes, Base64.DEFAULT)
}

// --- ARCHIVO TEMPORAL ---
private fun createTempImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

private fun File.toUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)

// --- LISTA DE REPORTES ---
@Composable
private fun MyReportsTab(reports: List<Report>) {
    if (reports.isEmpty()) {
        EmptyReportsState()
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(reports, key = { it.id }) { report ->
                ReportItemCard(report)
            }
        }
    }
}

@Composable
private fun ReportItemCard(report: Report) {
    val statusColor = when (report.status) {
        "pendiente" -> MaterialTheme.colorScheme.error
        "atendido" -> MaterialTheme.colorScheme.tertiary
        "resuelto" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
            Text(report.description, color = MaterialTheme.colorScheme.onSurfaceVariant)

            report.location?.let { gp ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Text("${String.format("%.6f", gp.latitude)}, ${String.format("%.6f", gp.longitude)}", style = MaterialTheme.typography.bodySmall)
                }
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
                        contentDescription = "Foto",
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReportsState() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Assignment, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            Text("No tienes reportes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Crea uno en la pestaña anterior", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}