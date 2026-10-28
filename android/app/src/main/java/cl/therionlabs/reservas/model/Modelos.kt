package cl.therionlabs.reservas.model

import java.time.LocalDate
import java.time.LocalTime

/** Modelos de dominio: lo único que ven las pantallas (nunca DTO ni Entity). */

enum class Rol { RELATOR, COORDINADOR }

enum class TipoSala(val etiqueta: String) {
    SALA("Sala de clases"),
    LABORATORIO("Laboratorio"),
    MULTIPROPOSITO("Multipropósito")
}

enum class EstadoReserva { ACTIVA, CANCELADA }

data class Usuario(
    val id: Long,
    val email: String,
    val nombre: String,
    val rol: Rol
) {
    val esCoordinador: Boolean get() = rol == Rol.COORDINADOR
}

data class Sala(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val tipo: TipoSala,
    val capacidad: Int,
    val proyector: Boolean,
    val computadores: Boolean,
    val climatizacion: Boolean,
    val activa: Boolean = true
)

data class Reserva(
    val id: Long,
    val salaId: Long,
    val salaCodigo: String,
    val salaNombre: String,
    val usuarioId: Long,
    val relator: String,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val actividad: String,
    val estado: EstadoReserva
) {
    val activa: Boolean get() = estado == EstadoReserva.ACTIVA

    /** Rangos semiabiertos [inicio, fin): 10-12 y 12-13 no chocan. */
    fun seSolapaCon(inicio: LocalTime, fin: LocalTime): Boolean =
        horaInicio < fin && inicio < horaFin
}

/** Criterios de búsqueda de salas; los valores false/null no filtran. */
data class FiltroSalas(
    val capacidadMin: Int? = null,
    val proyector: Boolean = false,
    val computadores: Boolean = false,
    val climatizacion: Boolean = false,
    val tipo: TipoSala? = null
) {
    fun cumple(sala: Sala): Boolean =
        sala.activa &&
            (tipo == null || sala.tipo == tipo) &&
            (capacidadMin == null || sala.capacidad >= capacidadMin) &&
            (!proyector || sala.proyector) &&
            (!computadores || sala.computadores) &&
            (!climatizacion || sala.climatizacion)
}

data class Ocupacion(
    val salaId: Long,
    val salaCodigo: String,
    val salaNombre: String,
    val minutosReservados: Long,
    val minutosDisponibles: Long,
    val porcentaje: Double,
    val cantidadReservas: Int
)

data class Conflicto(
    val tipo: String,
    val descripcion: String,
    val reserva: Reserva,
    val reservaEnConflicto: Reserva?
)
