package cl.therionlabs.reservas.model

import java.time.LocalTime

/**
 * Bloque de una hora de la grilla de agenda. Las reservas pueden durar varios bloques
 * o empezar a media hora: un bloque se considera ocupado si alguna reserva lo toca.
 */
data class BloqueHorario(val inicio: LocalTime, val fin: LocalTime) {

    val etiqueta: String get() = "%02d:%02d".format(inicio.hour, inicio.minute)

    companion object {
        val APERTURA: LocalTime = LocalTime.of(8, 0)
        val CIERRE: LocalTime = LocalTime.of(22, 0)

        /** 08:00-09:00 ... 21:00-22:00, igual que el horario configurado en el backend. */
        val DEL_DIA: List<BloqueHorario> =
            (APERTURA.hour until CIERRE.hour).map { h -> BloqueHorario(LocalTime.of(h, 0), LocalTime.of(h + 1, 0)) }
    }
}

/** Celda de la grilla: el bloque y la reserva activa que lo ocupa (null = libre). */
data class CeldaAgenda(val bloque: BloqueHorario, val reserva: Reserva?) {
    val libre: Boolean get() = reserva == null
}

data class FilaAgenda(val sala: Sala, val celdas: List<CeldaAgenda>) {

    val bloquesLibres: Int get() = celdas.count { it.libre }

    companion object {
        fun construir(sala: Sala, reservasDelDia: List<Reserva>): FilaAgenda {
            val propias = reservasDelDia.filter { it.salaId == sala.id && it.activa }
            val celdas = BloqueHorario.DEL_DIA.map { b ->
                CeldaAgenda(b, propias.firstOrNull { it.seSolapaCon(b.inicio, b.fin) })
            }
            return FilaAgenda(sala, celdas)
        }
    }
}
