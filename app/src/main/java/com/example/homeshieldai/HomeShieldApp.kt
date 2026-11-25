package com.example.homeshieldai

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homeshieldai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

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
    val context = LocalContext.current.applicationContext
    val estado: EstadoSistema = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val e = EstadoSistema()
                e.modoTema = leerModoTema(context)
                e.alCambiarTema = { guardarModoTema(context, it) }
                cargarTarifa(context, e)
                e.alGuardarTarifa = { guardarTarifa(context, e) }
                cargarAjustes(context, e)
                e.alGuardarConfig = { guardarAjustes(context, e) }
                @Suppress("UNCHECKED_CAST")
                return e as T
            }
        }
    )
    HomeShieldAITheme(estado.modoTema) {
        if (!estado.sesionIniciada) {
            PantallaLogin(estado)
        } else {
            val uid = estado.uidActual
            var vista by remember(uid) { mutableStateOf(bienvenidaVista(context, uid)) }
            if (vista) {
                AppPrincipal(estado)
            } else {
                PantallaBienvenida(estado) {
                    marcarBienvenidaVista(context, uid)
                    vista = true
                }
            }
        }
    }
}

@Composable
private fun MarcoDetalle(content: @Composable () -> Unit) {
    val contador = remember { ContadorAparicion() }
    Scaffold(containerColor = Color.Transparent, contentColor = TextoPrincipal) { padding ->
        CompositionLocalProvider(LocalContadorAparicion provides contador) {
            Box(Modifier.padding(padding)) { content() }
        }
    }
}

@Composable
fun AppPrincipal(estado: EstadoSistema) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { destinos.size })

    val context = LocalContext.current
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(estado.emergencia) {
        if (estado.emergencia) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(Unit) {
        val appContext = context.applicationContext
        crearCanalesNotificacion(appContext)
        estado.alAlerta = { titulo, mensaje, tono ->
            mostrarNotificacion(appContext, titulo, mensaje, tono)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        val appContext = context.applicationContext
        var ultimoEstado = -1
        var ultimaActualizacion = 0L
        while (true) {
            estado.tick()
            val codigo = when {
                estado.emergencia -> 2
                !estado.dispositivoConectado -> 1
                else -> 0
            }
            val ahoraMs = System.currentTimeMillis()
            if (codigo != ultimoEstado || ahoraMs - ultimaActualizacion > 30_000L) {
                guardarDatosWidget(
                    appContext,
                    DatosWidget(
                        estado = codigo,
                        potencia = estado.potencia,
                        gastoHoy = estado.gastoHoy,
                        tempTablero = estado.tempTablero,
                        hogar = estado.nombreHogar,
                        ts = ahoraMs,
                        gastoMes = estado.gastoMes,
                        presupuesto = estado.presupuesto
                    )
                )
                HomeShieldWidget().updateAll(appContext)
                ultimoEstado = codigo
                ultimaActualizacion = ahoraMs
            }
            delay(2000)
        }
    }

    fun irAPagina(i: Int) {
        scope.launch { pagerState.animateScrollToPage(i) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoAlto, Fondo)))
    ) {
        NavHost(
            navController = navController,
            startDestination = "principal",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, tween(300)
                ) + fadeIn(tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left, tween(300),
                    targetOffset = { it / 4 }
                ) + fadeOut(tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, tween(300),
                    initialOffset = { it / 4 }
                ) + fadeIn(tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right, tween(300)
                ) + fadeOut(tween(300))
            }
        ) {
            composable("principal") {
                BackHandler(enabled = pagerState.currentPage != 0) { irAPagina(0) }
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = TextoPrincipal,
                    bottomBar = {
                        BarraInferior(pagerState.currentPage) { irAPagina(it) }
                    }
                ) { padding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.padding(padding)
                    ) { pagina ->
                        val contador = remember { ContadorAparicion() }
                        CompositionLocalProvider(LocalContadorAparicion provides contador) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val d = ((pagerState.currentPage - pagina) +
                                                pagerState.currentPageOffsetFraction)
                                            .absoluteValue.coerceIn(0f, 1f)
                                        alpha = 1f - 0.5f * d
                                        val s = 1f - 0.06f * d
                                        scaleX = s
                                        scaleY = s
                                    }
                            ) {
                                when (pagina) {
                                    0 -> PantallaInicio(
                                        estado,
                                        onVerAlertas = { irAPagina(3) },
                                        onCuenta = { navController.navigate("cuenta") },
                                        onSensor = { id -> navController.navigate("sensor/$id") },
                                        onControl = { irAPagina(2) },
                                        onConsumo = { irAPagina(1) }
                                    )
                                    1 -> PantallaConsumo(estado)
                                    2 -> PantallaControl(estado) { id ->
                                        navController.navigate("dispositivo/$id")
                                    }
                                    3 -> PantallaAlertas(estado) { accion ->
                                        when (accion) {
                                            AccionAlerta.VER_CONSUMO -> irAPagina(1)
                                            AccionAlerta.VER_CONTROL -> irAPagina(2)
                                            AccionAlerta.IR_AJUSTES -> irAPagina(4)
                                            AccionAlerta.VER_TABLERO ->
                                                navController.navigate("sensor/tablero")
                                        }
                                    }
                                    else -> PantallaAjustes(
                                        estado,
                                        onCuenta = { navController.navigate("cuenta") },
                                        onTarifa = { navController.navigate("tarifa") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            composable("tarifa") {
                MarcoDetalle { PantallaTarifa(estado) { navController.popBackStack() } }
            }
            composable("cuenta") {
                MarcoDetalle { PantallaCuenta(estado) { navController.popBackStack() } }
            }
            composable(
                "sensor/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entrada ->
                MarcoDetalle {
                    PantallaSensor(
                        estado,
                        entrada.arguments?.getString("id") ?: "sala"
                    ) { navController.popBackStack() }
                }
            }
            composable(
                "dispositivo/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entrada ->
                MarcoDetalle {
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
fun BarraInferior(seleccionado: Int, onSeleccionar: (Int) -> Unit) {
    NavigationBar(containerColor = Superficie) {
        destinos.forEachIndexed { i, destino ->
            NavigationBarItem(
                selected = seleccionado == i,
                onClick = { onSeleccionar(i) },
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