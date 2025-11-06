package com.ronda.rondacajamarcaapp.ui.rondero

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun ReportScreen(
    user: User,
    reportVM: ReportViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val isTitleValid = title.isNotBlank() && title.length <= 100
    val isDescriptionValid = description.isNotBlank() && description.length <= 500
    val isFormValid = isTitleValid && isDescriptionValid

    // Cámara
    val photoFile = remember { createTempImageFile(context) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = photoFile.toUri(context)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = photoFile.toUri(context)
            cameraLauncher.launch(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crear Reporte", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Reporta incidencias en tu ronda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                // CORREGIDO: topBarColors to smallTopAppBarColors
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                        Text(
                            "Completa el formulario para reportar una incidencia. Tu ubicación será registrada automáticamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Información del Reporte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = title,
                            onValueChange = { if (it.length <= 100) title = it },
                            label = { Text("Título del reporte") },
                            placeholder = { Text("Ej: Luminaria dañada") },
                            leadingIcon = { Icon(Icons.Default.Title, null) },
                            supportingText = { Text("${title.length}/100 caracteres") },
                            isError = title.length > 100,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Campo de título del reporte" },
                            singleLine = true,
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 500) description = it },
                            label = { Text("Descripción detallada") },
                            placeholder = { Text("Describe la situación con el mayor detalle posible...") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            supportingText = { Text("${description.length}/500 caracteres") },
                            isError = description.length > 500,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).semantics { contentDescription = "Campo de descripción del reporte" },
                            minLines = 4,
                            maxLines = 8,
                            enabled = !isLoading
                        )

                        // BOTÓN CÁMARA
                        Button(
                            onClick = {
                                when {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                                        val uri = photoFile.toUri(context)
                                        cameraLauncher.launch(uri)
                                    }
                                    else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tomar Foto con Cámara")
                        }

                        // VISTA PREVIA
                        photoUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto del reporte",
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            TextButton(onClick = { photoUri = null }, modifier = Modifier.align(Alignment.End)) {
                                Text("Quitar foto")
                            }
                        }

                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            headlineContent = { Text("Reportado por") },
                            supportingContent = { Text(user.name) },
                            leadingContent = { Icon(Icons.Default.Person, null) }
                        )

                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            headlineContent = { Text("Ubicación") },
                            supportingContent = { Text("Se registrará automáticamente") },
                            leadingContent = { Icon(Icons.Default.LocationOn, null) }
                        )
                    }
                }

                // ENVIAR REPORTE
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
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
                                    title = ""
                                    description = ""
                                    photoUri = null
                                    showSuccess = true
                                    isLoading = false
                                } catch (e: Exception) {
                                    errorMessage = "Error al enviar: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = isFormValid && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Enviando...")
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar Reporte", fontWeight = FontWeight.SemiBold)
                    }
                }

                // ERROR
                AnimatedVisibility(visible = errorMessage != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            IconButton(onClick = { errorMessage = null }) {
                                Icon(Icons.Default.Close, "Cerrar", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ÉXITO
            if (showSuccess) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(3000); showSuccess = false }
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    action = { TextButton(onClick = { showSuccess = false }) { Text("OK") } }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Text("Reporte enviado exitosamente")
                    }
                }
            }
        }
    }
}

// --- BASE64 CONVERSIÓN ---
private suspend fun uriToBase64(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos) // < 300KB
        val bytes = baos.toByteArray()
        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        Log.e("BASE64_ERROR", "Error al convertir a Base64", e)
        throw e
    }
}

// --- ARCHIVO TEMPORAL ---
private fun createTempImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
}

private fun File.toUri(context: Context): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)