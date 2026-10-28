package cl.therionlabs.reservas.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.therionlabs.reservas.model.BloqueHorario
import cl.therionlabs.reservas.model.CeldaAgenda
import cl.therionlabs.reservas.model.FilaAgenda
import cl.therionlabs.reservas.ui.theme.ColoresAgenda

private val ALTO_FILA = 56.dp
private val ANCHO_SALA = 84.dp

/**
 * Grilla salas × bloques de una hora. La columna de salas queda fija y los bloques se
 * desplazan horizontalmente (una sola ScrollState para encabezado y filas, así quedan alineados).
 * Verde = libre (tocar para reservar), azul = ocupada, ámbar = reservada por mí, gris = ya pasó.
 */
@Composable
fun GrillaAgenda(
    filas: List<FilaAgenda>,
    usuarioId: Long,
    onCeldaLibre: (FilaAgenda, CeldaAgenda) -> Unit,
    onCeldaOcupada: (CeldaAgenda) -> Unit,
    modifier: Modifier = Modifier,
    anchoCelda: Dp = 72.dp,
    bloquePasado: (BloqueHorario) -> Boolean = { false }
) {
    val scroll = rememberScrollState()
    Row(modifier) {
        Column {
            Spacer(Modifier.height(32.dp))
            filas.forEach { fila ->
                Box(Modifier.size(ANCHO_SALA, ALTO_FILA).padding(end = 6.dp), contentAlignment = Alignment.CenterStart) {
                    Column {
                        Text(fila.sala.codigo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${fila.sala.capacidad} pers.", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Column(Modifier.horizontalScroll(scroll)) {
            Row(Modifier.height(32.dp)) {
                BloqueHorario.DEL_DIA.forEach { b ->
                    Text(b.etiqueta, Modifier.width(anchoCelda).padding(start = 4.dp),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            filas.forEach { fila ->
                Row(Modifier.height(ALTO_FILA)) {
                    fila.celdas.forEach { celda ->
                        val pasada = bloquePasado(celda.bloque)
                        Celda(celda, esPropia = celda.reserva?.usuarioId == usuarioId, pasada = pasada, ancho = anchoCelda) {
                            if (!celda.libre) onCeldaOcupada(celda) else if (!pasada) onCeldaLibre(fila, celda)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Celda(celda: CeldaAgenda, esPropia: Boolean, pasada: Boolean, ancho: Dp, onClick: () -> Unit) {
    val fondo: Color = when {
        celda.libre && pasada -> MaterialTheme.colorScheme.surfaceVariant
        celda.libre -> ColoresAgenda.libre()
        esPropia -> ColoresAgenda.propia()
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val descripcion = when {
        celda.reserva != null -> "${celda.bloque.etiqueta} ocupado: ${celda.reserva.actividad}"
        pasada -> "${celda.bloque.etiqueta} ya pasó"
        else -> "${celda.bloque.etiqueta} libre, tocar para reservar"
    }
    Box(
        Modifier
            .width(ancho)
            .height(ALTO_FILA)
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(fondo)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .clickable(enabled = !(celda.libre && pasada), onClick = onClick)
            .semantics { contentDescription = descripcion }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val r = celda.reserva
        if (r == null && pasada) {
            Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (r == null) {
            Text("Libre", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        } else {
            Column(verticalArrangement = Arrangement.Center) {
                Text(r.actividad, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Leyenda de colores bajo la grilla. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeyendaAgenda(modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ItemLeyenda(ColoresAgenda.libre(), "Libre")
        ItemLeyenda(MaterialTheme.colorScheme.primaryContainer, "Ocupada")
        ItemLeyenda(ColoresAgenda.propia(), "Mis reservas")
        ItemLeyenda(MaterialTheme.colorScheme.surfaceVariant, "Pasado")
    }
}

@Composable
private fun ItemLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall)
    }
}
