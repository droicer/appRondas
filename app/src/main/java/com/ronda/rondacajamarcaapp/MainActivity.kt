// File: app/src/main/java/com/ronda/rondacajamarcaapp/MainActivity.kt
package com.ronda.rondacajamarcaapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ronda.rondacajamarcaapp.auth.*
import com.ronda.rondacajamarcaapp.ui.admin.AdminScreen
import com.ronda.rondacajamarcaapp.ui.rondero.RonderoScreen
import com.ronda.rondacajamarcaapp.ui.user.UserScreen
import com.ronda.rondacajamarcaapp.ui.theme.RondaCajamarcaAppTheme
import com.ronda.rondacajamarcaapp.utils.LocationUtils

class MainActivity : ComponentActivity() {

    private val authVM: AuthViewModel by viewModels()
    private val reportVM: ReportViewModel by viewModels()
    private val emergencyVM: EmergencyViewModel by viewModels()

    private val multiplePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* opcional: logs */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf<String>()

        // UBICACIÓN
        if (!LocationUtils.hasLocationPermission(this)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // CÁMARA
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // NOTIFICACIONES (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            multiplePermissionLauncher.launch(permissions.toTypedArray())
        }

        setContent {
            RondaCajamarcaAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(authVM, reportVM, emergencyVM)
                }
            }
        }
    }
}

/** Navegación en memoria (solo para login/register) */
private sealed class Screen {
    object Login : Screen()
    object Register : Screen()
}

@Composable
private fun AppRoot(
    authVM: AuthViewModel,
    reportVM: ReportViewModel,
    emergencyVM: EmergencyViewModel
) {
    val currentUser by authVM.currentUser.collectAsState()
    val isLoading by authVM.isLoading.collectAsState()

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        currentUser != null -> {
            when (currentUser!!.role.lowercase()) {
                "admin" -> AdminScreen(
                    user = currentUser!!,
                    reportVM = reportVM,
                    emergencyVM = emergencyVM,
                    authVM = authVM,
                    onLogout = { authVM.logout() }
                )
                "rondero" -> RonderoScreen(
                    user = currentUser!!,
                    reportVM = reportVM,
                    emergencyVM = emergencyVM,
                    logout = { authVM.logout() }
                )
                else -> UserScreen(
                    user = currentUser!!,
                    reportVM = reportVM,
                    logout = { authVM.logout() }
                )
            }
        }

        else -> {
            var screen: Screen by remember { mutableStateOf(Screen.Login) }

            when (screen) {
                is Screen.Login -> AuthScreen(
                    authVM = authVM,
                    onLoginSuccess = { /* No necesitas hacer nada, el listener lo detecta */ },
                    onGoToRegister = { screen = Screen.Register }
                )
                is Screen.Register -> RegisterScreen(
                    authVM = authVM,
                    onRegisterSuccess = { screen = Screen.Login },
                    onBackToLogin = { screen = Screen.Login }
                )
            }
        }
    }
}