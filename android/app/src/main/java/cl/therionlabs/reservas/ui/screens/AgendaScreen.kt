package cl.therionlabs.reservas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.therionlabs.reservas.model.BloqueHorario
import cl.therionlabs.reservas.model.Reloj
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.Usuario
import cl.therionlabs.reservas.ui.componentes.Aviso
import cl.therionlabs.reservas.ui.componentes.GrillaAgenda
import cl.therionlabs.reservas.ui.componentes.LeyendaAgenda
import cl.therionlabs.reservas.ui.componentes.MensajeVacio
import cl.therionlabs.reservas.ui.componentes.SelectorFecha
import cl.therionlabs.reservas.ui.componentes.legible
import cl.therionlabs.reservas.viewmodel.AgendaViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Intervalo de sondeo mientras la agenda está en pantalla. */
private const val REFRESCO_MS = 30_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    usuario: Usuario,
    anchoCelda: Dp,
    onReservar: (salaId: Long, fecha: LocalDate, inicio: LocalTime, fin: LocalTime) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    val vm: AgendaViewModel = viewModel(factory = AgendaViewModel.fabrica(contexto))
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val filas by vm.filas.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val aviso by vm.aviso.collectAsStateWithLifecycle()
    var detalle by remember { mutableStateOf<Reserva?>(null) }
    var porCancelar by remember { mutableStateOf<Reserva?>(null) }
    val snackbar = remember { SnackbarHostState() }
    // Hora de la sede; se actualiza en cada sondeo para que los bloques que van terminando se apaguen.
    var ahora by remember { mutableStateOf(Reloj.ahora()) }

    // Se cancela solo al salir de la pantalla: el sondeo corre únicamente mientras se ve la agenda.
    LaunchedEffect(Unit) {
        while (true) {
            ahora = Reloj.ahora()
            vm.refrescar()
            delay(REFRESCO_MS)
        }
    }
    LaunchedEffect(vm) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = { BarraSuperior("Agenda de salas", onCerrarSesion = onCerrarSesion) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = sincronizando,
            onRefresh = { vm.refrescar(mostrarIndicador = true) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Hola, ${usuario.nombre}", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::diaAnterior, enabled = fecha > ahora.toLocalDate()) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
                    }
                    SelectorFecha("Fecha", fecha, vm::cambiarFecha, Modifier.weight(1f))
                    IconButton(onClick = vm::diaSiguiente) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
                    }
                }
                if (fecha != ahora.toLocalDate()) TextButton(onClick = vm::irAHoy) { Text("Volver a hoy") }
                aviso?.let { Aviso(it) }
                LeyendaAgenda()

                if (filas.isEmpty()) {
                    MensajeVacio("Sin datos de salas todavía. Desliza hacia abajo para actualizar.")
                } else {
                    // Un bloque terminado ya no se puede reservar: se pinta gris y no responde al toque.
                    val bloquePasado: (BloqueHorario) -> Boolean = { b -> LocalDateTime.of(fecha, b.fin) <= ahora }
                    val libres = filas.sumOf { f -> f.celdas.count { it.libre && !bloquePasado(it.bloque) } }
                    Text(
                        if (libres > 0) "$libres bloques libres · toca uno verde para reservarlo"
                        else "No quedan bloques libres este día. Prueba con el día siguiente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    GrillaAgenda(
                        filas = filas,
                        usuarioId = usuario.id,
                        anchoCelda = anchoCelda,
                        bloquePasado = bloquePasado,
                        onCeldaLibre = { fila, celda ->
                            // Si el bloque ya empezó, se propone desde el próximo cuarto de hora.
                            val hora = ahora.toLocalTime()
                            val inicio = if (fecha == ahora.toLocalDate() && hora > celda.bloque.inicio) {
                                LocalTime.of(hora.hour, hora.minute / 15 * 15).plusMinutes(15)
                            } else celda.bloque.inicio
                            val fin = if (inicio < celda.bloque.fin) celda.bloque.fin else inicio.plusHours(1)
                            onReservar(fila.sala.id, fecha, inicio, fin)
                        },
                        onCeldaOcupada = { detalle = it.reserva },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    detalle?.let { r ->
        AlertDialog(
            onDismissRequest = { detalle = null },
            title = { Text(r.actividad) },
            text = {
                Text(
                    "${r.salaCodigo} · ${r.salaNombre}\n" +
                        "${r.fecha.legible()}, ${r.horaInicio.legible()}–${r.horaFin.legible()}\n" +
                        "Relator: ${r.relator}"
                )
            },
            confirmButton = { TextButton(onClick = { detalle = null }) { Text("Cerrar") } },
            // El coordinador puede liberar cualquier reserva que aún no termina.
            dismissButton = if (usuario.esCoordinador && LocalDateTime.of(r.fecha, r.horaFin) > ahora) {
                {
                    TextButton(onClick = { porCancelar = r; detalle = null }) {
                        Text("Cancelar reserva", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else null
        )
    }

    porCancelar?.let { r ->
        AlertDialog(
            onDismissRequest = { porCancelar = null },
            title = { Text("¿Cancelar esta reserva?") },
            text = { Text("\"${r.actividad}\" de ${r.relator}, ${r.salaCodigo} ${r.horaInicio.legible()}–${r.horaFin.legible()}. El bloque quedará libre; avisa al relator para que reagende.") },
            confirmButton = { TextButton(onClick = { vm.cancelar(r); porCancelar = null }) { Text("Sí, cancelar") } },
            dismissButton = { TextButton(onClick = { porCancelar = null }) { Text("No") } }
        )
    }
}
