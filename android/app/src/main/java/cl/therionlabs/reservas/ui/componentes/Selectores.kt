package cl.therionlabs.reservas.ui.componentes

import cl.therionlabs.reservas.model.Reloj
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("EEE d 'de' MMMM", LOCALE_CL)

fun LocalDate.legible(): String = format(FORMATO_FECHA).replaceFirstChar { it.titlecase(LOCALE_CL) }
fun LocalDate.diaCorto(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, LOCALE_CL)
fun LocalTime.legible(): String = "%02d:%02d".format(hour, minute)

/** Botón con etiqueta y valor; abre un DatePicker de Material 3 que no permite días pasados. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    etiqueta: String,
    fecha: LocalDate,
    onCambio: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    permitirPasado: Boolean = false
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    CampoSelector(etiqueta, fecha.legible(), { abierto = true }, modifier)

    if (abierto) {
        // El DatePicker trabaja en milisegundos UTC a medianoche.
        val hoyUtc = Reloj.hoy().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = remember(permitirPasado) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = permitirPasado || utcTimeMillis >= hoyUtc
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        onCambio(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}

/** Botón que abre un TimePicker de Material 3 en un diálogo (formato 24 h). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorHora(
    etiqueta: String,
    hora: LocalTime,
    onCambio: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    CampoSelector(etiqueta, hora.legible(), { abierto = true }, modifier)

    if (abierto) {
        val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(etiqueta) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    onCambio(LocalTime.of(estado.hour, estado.minute))
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CampoSelector(etiqueta: String, valor: String, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelSmall)
            Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Fila Fecha + Inicio + Término, compartida por la búsqueda y el formulario de reserva. */
@Composable
fun SelectorRangoHorario(
    fecha: LocalDate,
    inicio: LocalTime,
    fin: LocalTime,
    onFecha: (LocalDate) -> Unit,
    onInicio: (LocalTime) -> Unit,
    onFin: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectorFecha("Fecha", fecha, onFecha, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectorHora("Inicio", inicio, onInicio, Modifier.weight(1f))
            SelectorHora("Término", fin, onFin, Modifier.weight(1f))
        }
    }
}

@Composable
fun EncabezadoFecha(texto: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.titleSmall)
    }
}
