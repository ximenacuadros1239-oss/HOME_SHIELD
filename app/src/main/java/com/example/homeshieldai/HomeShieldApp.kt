package com.example.homeshieldai

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.homeshieldai.ui.theme.*

data class Destino(val ruta: String, val titulo: String, val icono: ImageVector)

val destinos = listOf(
    Destino("inicio", "Inicio", Icons.Filled.Home),
    Destino("consumo", "Consumo", Icons.Filled.BarChart),
    Destino("alertas", "Alertas", Icons.Filled.Notifications),
    Destino("ajustes", "Ajustes", Icons.Filled.Settings)
)

@Composable
fun HomeShieldApp() {
    val context = LocalContext.current
    val estado = remember {
        EstadoSistema().also { e ->
            e.modoTema = leerModoTema(context)
            e.alCambiarTema = { guardarModoTema(context, it) }
        }
    }
    HomeShieldAITheme(estado.modoTema) {
        if (estado.sesionIniciada) AppPrincipal(estado) else PantallaLogin(estado)
    }
}

@Composable
fun AppPrincipal(estado: EstadoSistema) {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        crearCanalAlertas(context)
        estado.alAlerta = { mensaje -> mostrarNotificacionPeligro(context, mensaje) }
        if (Build.VERSION.SDK_INT >= 33) {
            lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoAlto, Fondo)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = TextoPrincipal,
            bottomBar = {
                BarraInferior(rutaActual = rutaActual) { ruta ->
                    navController.navigate(ruta) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "inicio",
                modifier = Modifier.padding(padding)
            ) {
                composable("inicio") {
                    PantallaInicio(
                        estado,
                        onVerAlertas = { navController.navigate("alertas") },
                        onCuenta = { navController.navigate("cuenta") }
                    )
                }
                composable("consumo") { PantallaConsumo(estado) }
                composable("alertas") { PantallaAlertas(estado) }
                composable("ajustes") { PantallaAjustes(estado) }
                composable("cuenta") {
                    PantallaCuenta(estado) { navController.popBackStack() }
                }
            }
        }
    }
}

@Composable
fun BarraInferior(rutaActual: String?, onSeleccionar: (String) -> Unit) {
    NavigationBar(containerColor = Superficie) {
        destinos.forEach { destino ->
            NavigationBarItem(
                selected = rutaActual == destino.ruta,
                onClick = { onSeleccionar(destino.ruta) },
                icon = { Icon(destino.icono, contentDescription = destino.titulo) },
                label = { Text(destino.titulo) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Menta,
                    selectedTextColor = Menta,
                    indicatorColor = MentaOscuro.copy(alpha = 0.5f),
                    unselectedIconColor = TextoSecundario,
                    unselectedTextColor = TextoSecundario
                )
            )
        }
    }
}