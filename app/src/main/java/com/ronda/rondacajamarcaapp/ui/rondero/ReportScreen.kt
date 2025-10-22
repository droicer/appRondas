package com.ronda.rondacajamarcaapp.ui.rondero

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.GeoPoint
import com.ronda.rondacajamarcaapp.auth.Report
import com.ronda.rondacajamarcaapp.auth.ReportViewModel
import com.ronda.rondacajamarcaapp.auth.User
import com.ronda.rondacajamarcaapp.utils.LocationUtils
import kotlinx.coroutines.launch

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

    val isTitleValid = title.isNotBlank()
    val isDescriptionValid = description.isNotBlank()
    val isFormValid = isTitleValid && isDescriptionValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Crear Reporte",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reporta incidencias en tu ronda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card de instrucciones
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Completa el formulario para reportar una incidencia. Tu ubicación será registrada automáticamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Formulario
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Información del Reporte",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Campo Título
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Título del reporte") },
                            placeholder = { Text("Ej: Luminaria dañada") },
                            leadingIcon = {
                                Icon(Icons.Default.Title, contentDescription = null)
                            },
                            supportingText = {
                                Text("${title.length}/100 caracteres")
                            },
                            isError = title.length > 100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "Campo de título del reporte"
                                },
                            singleLine = true,
                            enabled = !isLoading
                        )

                        // Campo Descripción
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción detallada") },
                            placeholder = { Text("Describe la situación con el mayor detalle posible...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,

                                )
                            },
                            supportingText = {
                                Text("${description.length}/500 caracteres")
                            },
                            isError = description.length > 500,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .semantics {
                                    contentDescription = "Campo de descripción del reporte"
                                },
                            minLines = 4,
                            maxLines = 8,
                            enabled = !isLoading
                        )

                        // Info de usuario
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            headlineContent = { Text("Reportado por") },
                            supportingContent = { Text(user.name) },
                            leadingContent = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        )

                        // Info de ubicación
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            headlineContent = { Text("Ubicación") },
                            supportingContent = { Text("Se registrará automáticamente") },
                            leadingContent = {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                            }
                        )
                    }
                }

                // Botón enviar
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null

                        LocationUtils.getCurrentLocation(context) { lat, lng ->
                            val report = Report(
                                title = title.trim(),
                                description = description.trim(),
                                status = "pendiente",
                                createdBy = user.uid,
                                createdByName = user.name,
                                location = GeoPoint(lat, lng)
                            )
                            scope.launch {
                                try {
                                    reportVM.createReport(report)
                                    title = ""
                                    description = ""
                                    showSuccess = true
                                    isLoading = false
                                } catch (e: Exception) {
                                    errorMessage = "Error al enviar el reporte: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Enviando reporte...")
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Enviar Reporte",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Mensaje de error
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { errorMessage = null }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Snackbar de éxito
            if (showSuccess) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    showSuccess = false
                }

                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    action = {
                        TextButton(onClick = { showSuccess = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Reporte enviado exitosamente")
                    }
                }
            }
        }
    }
}