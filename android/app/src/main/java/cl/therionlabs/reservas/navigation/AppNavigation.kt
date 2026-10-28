package cl.therionlabs.reservas.navigation

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.therionlabs.reservas.data.ServiceLocator
import cl.therionlabs.reservas.model.Usuario
import cl.therionlabs.reservas.ui.screens.AgendaScreen
import cl.therionlabs.reservas.ui.screens.BuscarSalasScreen
import cl.therionlabs.reservas.ui.screens.CatalogoSalasScreen
import cl.therionlabs.reservas.ui.screens.LoginScreen
import cl.therionlabs.reservas.ui.screens.MisReservasScreen
import cl.therionlabs.reservas.ui.screens.NuevaReservaScreen
import cl.therionlabs.reservas.ui.screens.PanelScreen
import cl.therionlabs.reservas.ui.screens.SalaFormScreen
import cl.therionlabs.reservas.viewmodel.ArgumentosReserva
import java.time.LocalDate
import java.time.LocalTime

private data class Destino(val screen: Screen, val etiqueta: String, val icono: ImageVector)

private val DESTINOS_RELATOR = listOf(
    Destino(Screen.Agenda, "Agenda", Icons.Filled.DateRange),
    Destino(Screen.Buscar, "Buscar", Icons.Filled.Search),
    Destino(Screen.MisReservas, "Mis reservas", Icons.AutoMirrored.Filled.List)
)

/** El coordinador también reserva, y además administra el catálogo y supervisa la ocupación. */
private val DESTINOS_COORDINADOR = DESTINOS_RELATOR + listOf(
    Destino(Screen.Catalogo, "Salas", Icons.Filled.Home),
    Destino(Screen.Panel, "Panel", Icons.Filled.Info)
)

/**
 * NavHost de la app. En teléfonos usa NavigationBar inferior; en tablets (ancho Medium/Expanded)
 * cambia a NavigationRail lateral y agranda las celdas de la agenda (Guía 9: adaptabilidad).
 */
@Composable
fun AppNavigation(
    anchoVentana: WindowWidthSizeClass,
    navController: NavHostController = rememberNavController()
) {
    val contexto = LocalContext.current
    val auth = remember { ServiceLocator.obtener(contexto).auth }
    var usuario by remember { mutableStateOf(auth.usuarioActual()) }
    val actual = usuario
    // Se decide una sola vez: si cambiara, NavHost reconstruiría el grafo y perdería la pila.
    val inicio = remember { if (actual == null) Screen.Login.route else Screen.Agenda.route }

    val permisoNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(actual?.id) {
        if (actual != null) permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val cerrarSesion: () -> Unit = {
        auth.cerrarSesion()
        usuario = null
        navController.navigate(Screen.Login.route) { popUpTo(navController.graph.id) { inclusive = true } }
    }
    val abrirReserva: (Long, LocalDate, LocalTime, LocalTime) -> Unit = { salaId, fecha, inicio, fin ->
        navController.navigate(Screen.NuevaReserva.crear(salaId, fecha, inicio, fin))
    }

    val destinos = if (actual?.esCoordinador == true) DESTINOS_COORDINADOR else DESTINOS_RELATOR
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val mostrarNavegacion = actual != null && destinos.any { it.screen.route == rutaActual }
    val compacto = anchoVentana == WindowWidthSizeClass.Compact
    val anchoCelda = if (compacto) 72.dp else 104.dp

    val irA: (Screen) -> Unit = { destino ->
        if (destino == Screen.Agenda) {
            // La Agenda es la base de la pila: se vuelve a ella desapilando. Navegar con restoreState
            // no sirve aquí, porque popUpTo(Agenda) asocia la Agenda al estado guardado de la última
            // pestaña y "restauraría" esa pestaña (p. ej. Mis reservas) en lugar de la Agenda.
            navController.popBackStack(Screen.Agenda.route, inclusive = false, saveState = true)
        } else {
            navController.navigate(destino.route) {
                // La Agenda queda siempre en la base de la pila tras iniciar sesión.
                popUpTo(Screen.Agenda.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (mostrarNavegacion && compacto) {
                NavigationBar {
                    destinos.forEach { d ->
                        NavigationBarItem(
                            selected = rutaActual == d.screen.route,
                            onClick = { irA(d.screen) },
                            icon = { Icon(d.icono, contentDescription = null) },
                            label = { Text(d.etiqueta) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (mostrarNavegacion && !compacto) {
                NavigationRail {
                    destinos.forEach { d ->
                        NavigationRailItem(
                            selected = rutaActual == d.screen.route,
                            onClick = { irA(d.screen) },
                            icon = { Icon(d.icono, contentDescription = null) },
                            label = { Text(d.etiqueta) }
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = inicio,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(onIngreso = { u ->
                        usuario = u
                        navController.navigate(Screen.Agenda.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                    })
                }
                // Las pantallas con sesión solo se dibujan si hay usuario (tras cerrar sesión, `actual` es null).
                composable(Screen.Agenda.route) {
                    actual?.let { AgendaScreen(it, anchoCelda, abrirReserva, cerrarSesion) }
                }
                composable(Screen.Buscar.route) {
                    BuscarSalasScreen(abrirReserva, cerrarSesion)
                }
                composable(Screen.MisReservas.route) {
                    actual?.let { u ->
                        MisReservasScreen(u, onEditar = { r ->
                            navController.navigate(
                                Screen.NuevaReserva.crear(r.salaId, r.fecha, r.horaInicio, r.horaFin, r.id)
                            )
                        }, onCerrarSesion = cerrarSesion)
                    }
                }
                composable(
                    Screen.NuevaReserva.route,
                    arguments = listOf(
                        navArgument("salaId") { type = NavType.LongType },
                        navArgument("fecha") { type = NavType.StringType },
                        navArgument("inicio") { type = NavType.StringType },
                        navArgument("fin") { type = NavType.StringType },
                        navArgument("reservaId") { type = NavType.LongType; defaultValue = -1L }
                    )
                ) { entry ->
                    val a = requireNotNull(entry.arguments)
                    val args = ArgumentosReserva(
                        salaId = a.getLong("salaId"),
                        fecha = LocalDate.parse(a.getString("fecha")),
                        inicio = LocalTime.parse(a.getString("inicio")),
                        fin = LocalTime.parse(a.getString("fin")),
                        reservaId = a.getLong("reservaId").takeIf { it > 0 }
                    )
                    actual?.let { u ->
                        NuevaReservaScreen(
                            usuario = u,
                            args = args,
                            onGuardada = {
                                Toast.makeText(contexto, "Reserva confirmada en ${it.salaCodigo}", Toast.LENGTH_SHORT).show()
                                // Sin saveState: el formulario ya guardado se descarta y no reaparece
                                // al volver a tocar una pestaña.
                                navController.navigate(Screen.MisReservas.route) {
                                    popUpTo(Screen.Agenda.route)
                                    launchSingleTop = true
                                }
                            },
                            onVolver = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Catalogo.route) {
                    CatalogoSalasScreen(
                        onEditar = { id -> navController.navigate(Screen.SalaForm.crear(id)) },
                        onCerrarSesion = cerrarSesion
                    )
                }
                composable(
                    Screen.SalaForm.route,
                    arguments = listOf(navArgument("salaId") { type = NavType.LongType; defaultValue = -1L })
                ) { entry ->
                    val salaId = entry.arguments?.getLong("salaId")?.takeIf { it > 0 }
                    SalaFormScreen(
                        salaId = salaId,
                        onListo = { navController.popBackStack() },
                        onVolver = { navController.popBackStack() }
                    )
                }
                composable(Screen.Panel.route) {
                    PanelScreen(cerrarSesion)
                }
            }
        }
    }
}
