package com.example.homeshieldai

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homeshieldai.ui.theme.*
import kotlinx.coroutines.delay

data class Destino(val ruta: String, val titulo: String, val icono: ImageVector)

val destinos = listOf(
    Destino("inicio", "Inicio", Icons.Filled.Home),
    Destino("consumo", "Consumo", Icons.Filled.BarChart),
    Destino("control", "Control", Icons.Filled.Power),
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
            cargarTarifa(context, e)
            e.alGuardarTarifa = { guardarTarifa(context, e) }
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

    LaunchedEffect(Unit) {
        while (true) {
            estado.tick()
            delay(2000)
        }
    }

    // Una sola forma de ir a una pestaña: vuelve siempre a Inicio al retroceder
    fun irAPestana(ruta: String) {
        navController.navigate(ruta) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
            launchSingleTop = true
        }
    }

    val rutaResaltada = when {
        rutaActual?.startsWith("sensor") == true || rutaActual == "cuenta" -> "inicio"
        rutaActual?.startsWith("dispositivo") == true -> "control"
        rutaActual == "tarifa" -> "consumo"
        else -> rutaActual
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
                BarraInferior(rutaActual = rutaResaltada) { ruta ->
                    if (ruta != rutaActual) irAPestana(ruta)
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "inicio",
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(150)) }
            ) {
                composable("inicio") {
                    PantallaInicio(
                        estado,
                        onVerAlertas = { irAPestana("alertas") },
                        onCuenta = { navController.navigate("cuenta") },
                        onSensor = { id -> navController.navigate("sensor/$id") },
                        onControl = { irAPestana("control") },
                        onTarifa = { navController.navigate("tarifa") }
                    )
                }
                composable("consumo") {
                    PantallaConsumo(estado) { navController.navigate("tarifa") }
                }
                composable("tarifa") {
                    PantallaTarifa(estado) { navController.popBackStack() }
                }
                composable("control") {
                    PantallaControl(estado) { id -> navController.navigate("dispositivo/$id") }
                }
                composable("alertas") { PantallaAlertas(estado) }
                composable("ajustes") { PantallaAjustes(estado) }
                composable("cuenta") {
                    PantallaCuenta(estado) { navController.popBackStack() }
                }
                composable(
                    "sensor/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entrada ->
                    PantallaSensor(
                        estado,
                        entrada.arguments?.getString("id") ?: "sala"
                    ) { navController.popBackStack() }
                }
                composable(
                    "dispositivo/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { entrada ->
                    PantallaDispositivo(
                        estado,
                        entrada.arguments?.getInt("id") ?: -1
                    ) { navController.popBackStack() }
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