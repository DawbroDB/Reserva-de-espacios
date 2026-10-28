package cl.therionlabs.reservas.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Hora oficial de la sede (Viña del Mar), la misma que usa el backend. No se usa la zona del
 * teléfono: un equipo configurado en GMT (como el emulador por defecto) vería los bloques de
 * hoy como "pasados" o permitiría reservar horas que en la sede ya transcurrieron.
 */
object Reloj {
    val ZONA_SEDE: ZoneId = ZoneId.of("America/Santiago")

    fun ahora(): LocalDateTime = LocalDateTime.now(ZONA_SEDE)
    fun hoy(): LocalDate = LocalDate.now(ZONA_SEDE)
    fun hora(): LocalTime = LocalTime.now(ZONA_SEDE)
}
