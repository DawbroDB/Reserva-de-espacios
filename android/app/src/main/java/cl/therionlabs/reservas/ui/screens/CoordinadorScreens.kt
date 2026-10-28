package cl.therionlabs.reservas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.therionlabs.reservas.model.Conflicto
import cl.therionlabs.reservas.model.Ocupacion
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.TipoSala
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.ui.componentes.Aviso
import cl.therionlabs.reservas.ui.componentes.MensajeVacio
import cl.therionlabs.reservas.ui.componentes.TarjetaReserva
import cl.therionlabs.reservas.ui.componentes.TarjetaSala
import cl.therionlabs.reservas.ui.componentes.legible
import cl.therionlabs.reservas.viewmodel.CatalogoSalasViewModel
import cl.therionlabs.reservas.viewmodel.EventoFormulario
import cl.therionlabs.reservas.viewmodel.PanelViewModel
import cl.therionlabs.reservas.viewmodel.SalaFormViewModel

// ---------- Catálogo de espacios ----------

@Composable
fun CatalogoSalasScreen(onEditar: (salaId: Long?) -> Unit, onCerrarSesion: () -> Unit) {
    val contexto = LocalContext.current
    val vm: CatalogoSalasViewModel = viewModel(factory = CatalogoSalasViewModel.fabrica(contexto))
    val salas by vm.lista.collectAsStateWithLifecycle()
    val aviso by vm.aviso.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refrescar() }

    Scaffold(
        topBar = { BarraSuperior("Catálogo de espacios", onCerrarSesion = onCerrarSesion) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditar(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva sala") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            aviso?.let { item { Aviso(it) } }
            item {
                Text("${salas.count { it.activa }} activas · ${salas.count { !it.activa }} fuera de servicio",
                    style = MaterialTheme.typography.bodySmall)
            }
            items(salas, key = { it.id }) { sala ->
                TarjetaSala(sala, detalle = "Toca para editar") { onEditar(sala.id) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalaFormScreen(salaId: Long?, onListo: () -> Unit, onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val vm: SalaFormViewModel = viewModel(key = "sala-$salaId", factory = SalaFormViewModel.fabrica(contexto, salaId))
    val e by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmarBaja by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.eventos.collect { ev ->
            when (ev) {
                EventoFormulario.Listo -> onListo()
                is EventoFormulario.Fallo -> snackbar.showSnackbar(ev.mensaje)
            }
        }
    }

    Scaffold(
        topBar = { BarraSuperior(if (e.esNueva) "Nueva sala" else "Editar ${e.codigo}", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                e.codigo, { v -> vm.actualizar { copy(codigo = v) } },
                label = { Text("Código (ej. LAB-05)") }, singleLine = true,
                isError = "codigo" in e.errores, supportingText = { e.errores["codigo"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                e.nombre, { v -> vm.actualizar { copy(nombre = v.take(80)) } },
                label = { Text("Nombre") }, singleLine = true,
                isError = "nombre" in e.errores, supportingText = { e.errores["nombre"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                e.capacidad, { v -> vm.actualizar { copy(capacidad = v.filter(Char::isDigit).take(3)) } },
                label = { Text("Capacidad máxima de alumnos") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = "capacidad" in e.errores, supportingText = { e.errores["capacidad"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Tipo de espacio", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoSala.entries.forEach { t ->
                    FilterChip(e.tipo == t, { vm.actualizar { copy(tipo = t) } }, label = { Text(t.etiqueta) })
                }
            }
            Text("Equipamiento", style = MaterialTheme.typography.labelLarge)
            FilaSwitch("Proyector", e.proyector) { v -> vm.actualizar { copy(proyector = v) } }
            FilaSwitch("Computadores", e.computadores) { v -> vm.actualizar { copy(computadores = v) } }
            FilaSwitch("Climatización", e.climatizacion) { v -> vm.actualizar { copy(climatizacion = v) } }
            FilaSwitch("Disponible para reservas", e.activa) { v -> vm.actualizar { copy(activa = v) } }

            Button(onClick = vm::guardar, enabled = !e.enviando, modifier = Modifier.fillMaxWidth()) {
                Text(if (e.esNueva) "Crear sala" else "Guardar cambios")
            }
            if (!e.esNueva && e.activa) {
                OutlinedButton(onClick = { confirmarBaja = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Dejar fuera de servicio")
                }
            }
        }
    }

    if (confirmarBaja) {
        AlertDialog(
            onDismissRequest = { confirmarBaja = false },
            title = { Text("¿Dejar ${e.codigo} fuera de servicio?") },
            text = { Text("No se podrán crear reservas nuevas. Las reservas futuras existentes aparecerán como inconsistencias en el Panel para reasignarlas.") },
            confirmButton = { TextButton(onClick = { confirmarBaja = false; vm.desactivar() }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { confirmarBaja = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FilaSwitch(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}

// ---------- Panel: ocupación + inconsistencias ----------

@Composable
fun PanelScreen(onCerrarSesion: () -> Unit) {
    val contexto = LocalContext.current
    val vm: PanelViewModel = viewModel(factory = PanelViewModel.fabrica(contexto))
    val ocupacion by vm.ocupacion.collectAsStateWithLifecycle()
    val conflictos by vm.conflictos.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var porCancelar by remember { mutableStateOf<Reserva?>(null) }

    LaunchedEffect(Unit) {
        vm.refrescar()
        vm.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = { BarraSuperior("Panel de infraestructura", onCerrarSesion = onCerrarSesion) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val total = (conflictos as? ResultadoApi.Exito)?.datos?.size
            TabRow(selectedTabIndex = pestana) {
                Tab(pestana == 0, { pestana = 0 }, text = { Text("Ocupación") })
                Tab(pestana == 1, { pestana = 1 }, text = { Text("Inconsistencias" + (total?.let { " ($it)" } ?: "")) })
            }
            when (pestana) {
                0 -> ListaResultado(ocupacion, "Sin datos de ocupación") { lista ->
                    item {
                        Text("Del ${vm.desde.legible()} al ${vm.hasta.legible()} (08:00–22:00)",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    items(lista, key = { it.salaId }) { BarraOcupacion(it) }
                }
                else -> ListaResultado(conflictos, "") { lista ->
                    if (lista.isEmpty()) item { MensajeVacio("Sin inconsistencias: no hay topes ni reservas en salas fuera de servicio.") }
                    items(lista) { c -> TarjetaConflicto(c, onCancelar = { porCancelar = it }) }
                }
            }
        }
    }

    porCancelar?.let { r ->
        AlertDialog(
            onDismissRequest = { porCancelar = null },
            title = { Text("¿Cancelar esta reserva?") },
            text = { Text("\"${r.actividad}\" de ${r.relator}, ${r.salaCodigo} ${r.horaInicio.legible()}–${r.horaFin.legible()}. Avisa al relator para que reagende.") },
            confirmButton = { TextButton(onClick = { vm.cancelar(r); porCancelar = null }) { Text("Cancelar reserva") } },
            dismissButton = { TextButton(onClick = { porCancelar = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun <T> ListaResultado(
    resultado: ResultadoApi<List<T>>,
    vacio: String,
    contenido: androidx.compose.foundation.lazy.LazyListScope.(List<T>) -> Unit
) {
    when (resultado) {
        ResultadoApi.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is ResultadoApi.Error -> Aviso(resultado.mensaje, Modifier.padding(16.dp))
        is ResultadoApi.Exito -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (resultado.datos.isEmpty() && vacio.isNotEmpty()) item { MensajeVacio(vacio) }
            contenido(resultado.datos)
        }
    }
}

@Composable
private fun BarraOcupacion(o: Ocupacion) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.salaCodigo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(72.dp))
                Text(o.salaNombre, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("${o.porcentaje}%", style = MaterialTheme.typography.titleSmall)
            }
            LinearProgressIndicator(
                progress = { (o.porcentaje / 100).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text("${o.cantidadReservas} reservas · ${o.minutosReservados / 60} h de ${o.minutosDisponibles / 60} h",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TarjetaConflicto(c: Conflicto, onCancelar: (Reserva) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (c.tipo == "TOPE_HORARIO") "Tope de horario" else "Sala fuera de servicio",
                style = MaterialTheme.typography.titleSmall)
            Text(c.descripcion, style = MaterialTheme.typography.bodySmall)
            TarjetaReserva(c.reserva) { TextButton(onClick = { onCancelar(c.reserva) }) { Text("Cancelar esta") } }
            c.reservaEnConflicto?.let { otra ->
                TarjetaReserva(otra) { TextButton(onClick = { onCancelar(otra) }) { Text("Cancelar esta") } }
            }
        }
    }
}
