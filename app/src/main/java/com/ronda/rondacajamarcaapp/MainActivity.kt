package com.ronda.rondacajamarcaapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permiso de ubicación si falta
        if (!LocationUtils.hasLocationPermission(this)) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

/** Navegación en memoria */
private sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    data class Admin(val user: User) : Screen()
    data class Rondero(val user: User) : Screen()
    data class UserHome(val user: User) : Screen()
}

@Composable
private fun AppRoot(
    authVM: AuthViewModel,
    reportVM: ReportViewModel,
    emergencyVM: EmergencyViewModel
) {
    var screen: Screen by remember { mutableStateOf(Screen.Login) }
    var currentUser: User? by remember { mutableStateOf(null) }

    when (val s = screen) {
        is Screen.Login -> AuthScreen(
            authVM = authVM,
            onLoginSuccess = { user ->
                currentUser = user
                screen = when (user.role.lowercase()) {
                    "admin" -> Screen.Admin(user)
                    "rondero" -> Screen.Rondero(user)
                    else -> Screen.UserHome(user)
                }
            },
            onGoToRegister = { screen = Screen.Register }
        )

        is Screen.Register -> RegisterScreen(
            authVM = authVM,
            onRegisterSuccess = { screen = Screen.Login },
            onBackToLogin = { screen = Screen.Login }
        )

        is Screen.Admin -> AdminScreen(
            user = s.user,
            reportVM = reportVM,
            emergencyVM = emergencyVM,
            authVM = authVM,
            onLogout = {
                currentUser = null
                screen = Screen.Login
            }
        )

        is Screen.Rondero -> RonderoScreen(
            user = s.user,
            reportVM = reportVM,
            emergencyVM = emergencyVM,
            logout = {
                currentUser = null
                screen = Screen.Login
            }
        )

        is Screen.UserHome -> UserScreen(
            user = s.user,
            reportVM = reportVM,
            logout = {
                currentUser = null
                screen = Screen.Login
            }
        )
    }
}
