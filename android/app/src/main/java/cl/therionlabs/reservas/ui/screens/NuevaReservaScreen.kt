package cl.therionlabs.reservas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.Usuario
import cl.therionlabs.reservas.notificaciones.RecordatoriosWorkManager
import cl.therionlabs.reservas.ui.componentes.Aviso
import cl.therionlabs.reservas.ui.componentes.SelectorRangoHorario
import cl.therionlabs.reservas.ui.componentes.TarjetaReserva
import cl.therionlabs.reservas.ui.componentes.TarjetaSala
import cl.therionlabs.reservas.viewmodel.ArgumentosReserva
import cl.therionlabs.reservas.viewmodel.EventoReserva
import cl.therionlabs.reservas.viewmodel.NuevaReservaViewModel

@Composable
fun NuevaReservaScreen(
    usuario: Usuario,
    args: ArgumentosReserva,
    onGuardada: (Reserva) -> Unit,
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current
    // La key incluye los argumentos: abrir el formulario con otra sala crea un ViewModel nuevo.
    val vm: NuevaReservaViewModel = viewModel(
        key = "reserva-$args",
        factory = NuevaReservaViewModel.fabrica(contexto, usuario.id, args)
    )
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.eventos.collect { e ->
            when (e) {
                is EventoReserva.Guardada -> onGuardada(e.reserva)
                is EventoReserva.Mensaje -> snackbar.showSnackbar(e.texto)
            }
        }
    }

    Scaffold(
        topBar = { BarraSuperior(if (estado.editando) "Modificar reserva" else "Nueva reserva", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            estado.sala?.let { TarjetaSala(it) }
            OutlinedTextField(
                value = estado.actividad,
                onValueChange = vm::cambiarActividad,
                label = { Text("Curso o actividad") },
                singleLine = true,
                isError = estado.errorActividad != null,
                supportingText = { Text(estado.errorActividad ?: "${estado.actividad.length}/120") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Relator: ${usuario.nombre}", style = MaterialTheme.typography.bodyMedium)
            SelectorRangoHorario(
                estado.fecha, estado.inicio, estado.fin,
                vm::cambiarFecha, vm::cambiarInicio, vm::cambiarFin
            )
            estado.errorHorario?.let { Aviso(it) }
            estado.conflicto?.let {
                Text("Reserva existente:", style = MaterialTheme.typography.labelLarge)
                TarjetaReserva(it)
            }
            Text(
                "Recibirás un aviso ${RecordatoriosWorkManager.MINUTOS_ANTES} minutos antes del inicio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = vm::guardar, enabled = !estado.enviando, modifier = Modifier.fillMaxWidth()) {
                if (estado.enviando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (estado.editando) "Guardar cambios" else "Confirmar reserva")
            }
        }
    }
}
