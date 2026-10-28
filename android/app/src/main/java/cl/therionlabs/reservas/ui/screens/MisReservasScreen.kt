package cl.therionlabs.reservas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.Usuario
import cl.therionlabs.reservas.ui.componentes.MensajeVacio
import cl.therionlabs.reservas.ui.componentes.SelectorFecha
import cl.therionlabs.reservas.ui.componentes.TarjetaReserva
import cl.therionlabs.reservas.ui.componentes.legible
import cl.therionlabs.reservas.viewmodel.MisReservasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisReservasScreen(
    usuario: Usuario,
    onEditar: (Reserva) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    val vm: MisReservasViewModel = viewModel(factory = MisReservasViewModel.fabrica(contexto, usuario.id))
    val lista by vm.lista.collectAsStateWithLifecycle()
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var porCancelar by remember { mutableStateOf<Reserva?>(null) }

    LaunchedEffect(Unit) {
        vm.refrescar()
        vm.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = { BarraSuperior("Mis reservas", onCerrarSesion = onCerrarSesion) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        PullToRefreshBox(sincronizando, onRefresh = vm::refrescar, Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filtro != null,
                            onClick = { if (filtro == null) vm.activarFiltro() else vm.quitarFiltro() },
                            label = { Text(if (filtro == null) "Filtrar por fechas" else "Quitar filtro") },
                            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
                        )
                        filtro?.let { f ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectorFecha("Desde", f.desde, vm::cambiarDesde, Modifier.weight(1f), permitirPasado = true)
                                SelectorFecha("Hasta", f.hasta, vm::cambiarHasta, Modifier.weight(1f), permitirPasado = true)
                            }
                        }
                    }
                }
                item { Text("Próximas", style = MaterialTheme.typography.titleMedium) }
                if (lista.proximas.isEmpty()) {
                    item {
                        MensajeVacio(
                            if (filtro != null) "No tienes reservas próximas en esas fechas."
                            else "No tienes reservas próximas. Agenda una desde la Agenda o Buscar."
                        )
                    }
                }
                items(lista.proximas, key = { it.id }) { r ->
                    TarjetaReserva(r) {
                        TextButton(onClick = { onEditar(r) }) { Text("Modificar") }
                        TextButton(onClick = { porCancelar = r }) { Text("Cancelar") }
                    }
                }
                if (lista.pasadas.isNotEmpty()) {
                    item { Text("Historial reciente", style = MaterialTheme.typography.titleMedium) }
                    items(lista.pasadas, key = { it.id }) { r -> TarjetaReserva(r) }
                }
            }
        }
    }

    porCancelar?.let { r ->
        AlertDialog(
            onDismissRequest = { porCancelar = null },
            title = { Text("¿Cancelar la reserva?") },
            text = { Text("\"${r.actividad}\" en ${r.salaCodigo}, ${r.fecha.legible()} ${r.horaInicio.legible()}. El bloque quedará libre para otros relatores.") },
            confirmButton = {
                TextButton(onClick = { vm.cancelar(r); porCancelar = null }) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { porCancelar = null }) { Text("No") } }
        )
    }
}
