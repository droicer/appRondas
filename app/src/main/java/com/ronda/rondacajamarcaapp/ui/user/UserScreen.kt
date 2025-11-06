package com.ronda.rondacajamarcaapp.ui.user

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.firestore.GeoPoint
import com.ronda.rondacajamarcaapp.auth.Report
import com.ronda.rondacajamarcaapp.auth.ReportViewModel
import com.ronda.rondacajamarcaapp.auth.User
import com.ronda.rondacajamarcaapp.utils.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    user: User,
    reportVM: ReportViewModel,
    logout: () -> Unit
) {
    val reports by reportVM.userReports.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isCreatingReport by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPermissionDenied by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    val photoFile = remember { createTempImageFile(context) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoUri = photoFile.toUri(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(photoFile.toUri(context))
        else showPermissionDenied = true
    }

    LaunchedEffect(Unit) {
        reportVM.listenUserReports(user.uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hola, ${user.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Usuario", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                // CORREGIDO: topBarColors → smallTopAppBarColors
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isCreatingReport = !isCreatingReport },
                icon = { Icon(if (isCreatingReport) Icons.Default.Close else Icons.Default.Add, null) },
                text = { Text(if (isCreatingReport) "Cancelar" else "Nuevo Reporte") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isCreatingReport) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Report, null, tint = Color.White)
                                }
                                Text("Crear Nuevo Reporte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = title, onValueChange = { title = it },
                                label = { Text("Título del reporte") },
                                leadingIcon = { Icon(Icons.Default.Title, null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = desc, onValueChange = { desc = it },
                                label = { Text("Descripción detallada") },
                                leadingIcon = { Icon(Icons.Default.Description, null) },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4
                            )

                            // BOTÓN CÁMARA
                            if (hasCamera) {
                                Button(
                                    onClick = {
                                        when {
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                                    PackageManager.PERMISSION_GRANTED -> {
                                                cameraLauncher.launch(photoFile.toUri(context))
                                            }
                                            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Tomar Foto")
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Cámara no disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // VISTA PREVIA
                            photoUri?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Foto",
                                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                TextButton(onClick = { photoUri = null }, modifier = Modifier.align(Alignment.End)) {
                                    Text("Quitar foto")
                                }
                            }

                            // ENVIAR REPORTE
                            Button(
                                onClick = {
                                    if (title.isNotBlank() && desc.isNotBlank()) {
                                        LocationUtils.getCurrentLocation(context) { lat, lng ->
                                            scope.launch {
                                                try {
                                                    val photoBase64 = photoUri?.let { uri -> uriToBase64(uri, context) }

                                                    val report = Report(
                                                        title = title.trim(),
                                                        description = desc.trim(),
                                                        createdBy = user.uid,
                                                        createdByName = user.name,
                                                        location = GeoPoint(lat, lng),
                                                        photoBase64 = photoBase64
                                                    )
                                                    reportVM.createReport(report)
                                                    title = ""; desc = ""; photoUri = null; isCreatingReport = false
                                                } catch (e: Exception) {
                                                    // Manejar error
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = title.isNotBlank() && desc.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Enviar Reporte")
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mis Reportes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary) {
                        Text("${reports.size}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (reports.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Text("No tienes reportes aún", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Crea tu primer reporte", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            } else {
                items(reports) { report ->
                    ReportCard(report = report)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Diálogo de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, null) },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro?") },
            confirmButton = { TextButton(onClick = { showLogoutDialog = false; logout() }) { Text("Sí") } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") } }
        )
    }

    // Diálogo de permiso denegado
    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Permiso requerido") },
            text = { Text("Necesitas permitir el acceso a la cámara.") },
            confirmButton = { TextButton(onClick = { showPermissionDenied = false }) { Text("OK") } }
        )
    }
}

// --- ReportCard con foto en Base64 ---
@Composable
private fun ReportCard(report: Report) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(report.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (report.status.lowercase()) {
                        "pendiente" -> MaterialTheme.colorScheme.errorContainer
                        "en proceso" -> MaterialTheme.colorScheme.tertiaryContainer
                        "resuelto" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Icon(
                            when (report.status.lowercase()) {
                                "pendiente" -> Icons.Default.Schedule
                                "en proceso" -> Icons.Default.Autorenew
                                "resuelto" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Info
                            },
                            null, modifier = Modifier.size(16.dp)
                        )
                        Text(report.status, style = MaterialTheme.typography.labelMedium)
                    }
                }

                report.location?.let { gp ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("%.4f, %.4f".format(gp.latitude, gp.longitude), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // MOSTRAR FOTO EN BASE64
            report.photoBase64?.let { base64 ->
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
        }
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