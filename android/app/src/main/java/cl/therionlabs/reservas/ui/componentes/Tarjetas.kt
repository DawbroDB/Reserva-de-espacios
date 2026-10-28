package cl.therionlabs.reservas.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.Sala

/** Chips de solo lectura con la capacidad y el equipamiento de la sala. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsEquipamiento(sala: Sala, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(onClick = {}, label = { Text("${sala.capacidad} personas") })
        if (sala.proyector) AssistChip(onClick = {}, label = { Text("Proyector") })
        if (sala.computadores) AssistChip(onClick = {}, label = { Text("Computadores") })
        if (sala.climatizacion) AssistChip(onClick = {}, label = { Text("Climatización") })
    }
}

@Composable
fun TarjetaSala(
    sala: Sala,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val contenido: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sala.codigo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(sala.tipo.etiqueta, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!sala.activa) {
                    Spacer(Modifier.weight(1f))
                    Text("Fuera de servicio", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(sala.nombre, style = MaterialTheme.typography.bodyLarge)
            ChipsEquipamiento(sala)
            if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { contenido() }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { contenido() }
    }
}

@Composable
fun TarjetaReserva(
    reserva: Reserva,
    modifier: Modifier = Modifier,
    acciones: @Composable (() -> Unit)? = null
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(reserva.actividad, style = MaterialTheme.typography.titleMedium)
            Text(
                "${reserva.fecha.legible()} · ${reserva.horaInicio.legible()}–${reserva.horaFin.legible()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${reserva.salaCodigo} · ${reserva.salaNombre} · ${reserva.relator}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (acciones != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { acciones() }
        }
    }
}

/** Franja de aviso (sin conexión, errores de sincronización). */
@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MensajeVacio(texto: String, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(texto, Modifier.padding(24.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
