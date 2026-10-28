package cl.therionlabs.reservas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.therionlabs.reservas.model.TipoSala
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.ui.componentes.Aviso
import cl.therionlabs.reservas.ui.componentes.MensajeVacio
import cl.therionlabs.reservas.ui.componentes.SelectorRangoHorario
import cl.therionlabs.reservas.ui.componentes.TarjetaSala
import cl.therionlabs.reservas.ui.componentes.legible
import cl.therionlabs.reservas.viewmodel.BuscarSalasViewModel
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuscarSalasScreen(
    onReservar: (salaId: Long, fecha: LocalDate, inicio: LocalTime, fin: LocalTime) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    val vm: BuscarSalasViewModel = viewModel(factory = BuscarSalasViewModel.fabrica(contexto))
    val estado by vm.estado.collectAsStateWithLifecycle()
    val resultado by vm.resultado.collectAsStateWithLifecycle()

    Scaffold(topBar = { BarraSuperior("Buscar sala disponible", onCerrarSesion = onCerrarSesion) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SelectorRangoHorario(
                    estado.fecha, estado.inicio, estado.fin,
                    vm::cambiarFecha, vm::cambiarInicio, vm::cambiarFin
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroEquipo("Todas", estado.tipo == null) { vm.cambiarTipo(null) }
                    TipoSala.entries.forEach { t ->
                        FiltroEquipo(t.etiqueta, estado.tipo == t) { vm.cambiarTipo(t) }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = estado.capacidad,
                    onValueChange = vm::cambiarCapacidad,
                    label = { Text("Capacidad mínima (alumnos)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroEquipo("Proyector", estado.proyector, vm::alternarProyector)
                    FiltroEquipo("Computadores", estado.computadores, vm::alternarComputadores)
                    FiltroEquipo("Climatización", estado.climatizacion, vm::alternarClimatizacion)
                }
            }
            item {
                estado.error?.let { Aviso(it, Modifier.padding(bottom = 8.dp)) }
                Button(onClick = vm::buscar, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Text("  Buscar salas libres")
                }
            }

            when (val r = resultado) {
                null -> Unit
                ResultadoApi.Cargando -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ResultadoApi.Error -> item { Aviso(r.mensaje) }
                is ResultadoApi.Exito -> {
                    item {
                        Text(
                            "${r.datos.size} salas libres el ${estado.fecha.legible()} de " +
                                "${estado.inicio.legible()} a ${estado.fin.legible()}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (r.datos.isEmpty()) item { MensajeVacio("No hay salas que cumplan los filtros en ese horario.") }
                    items(r.datos, key = { it.id }) { sala ->
                        TarjetaSala(sala, detalle = "Toca para reservar") {
                            onReservar(sala.id, estado.fecha, estado.inicio, estado.fin)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltroEquipo(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (seleccionado) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else null
    )
}
