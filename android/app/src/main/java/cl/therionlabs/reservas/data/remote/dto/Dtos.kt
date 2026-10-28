package cl.therionlabs.reservas.data.remote.dto

/**
 * Forma exacta del JSON del backend Spring Boot. Fechas y horas viajan como texto ISO
 * ("2026-10-28", "14:00:00") y se convierten a java.time en Mappers.kt.
 */

data class LoginRequestDto(val email: String, val password: String)

data class UsuarioDto(val id: Long, val email: String, val nombre: String, val rol: String)

data class SalaDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val tipo: String,
    val capacidad: Int,
    val proyector: Boolean,
    val computadores: Boolean,
    val climatizacion: Boolean,
    val activa: Boolean
)

data class SalaRequestDto(
    val codigo: String,
    val nombre: String,
    val tipo: String,
    val capacidad: Int,
    val proyector: Boolean,
    val computadores: Boolean,
    val climatizacion: Boolean,
    val activa: Boolean
)

data class ReservaDto(
    val id: Long,
    val salaId: Long,
    val salaCodigo: String,
    val salaNombre: String,
    val usuarioId: Long,
    val relator: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val actividad: String,
    val estado: String
)

data class ReservaRequestDto(
    val salaId: Long,
    val usuarioId: Long,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val actividad: String
)

data class OcupacionDto(
    val salaId: Long,
    val salaCodigo: String,
    val salaNombre: String,
    val minutosReservados: Long,
    val minutosDisponibles: Long,
    val porcentaje: Double,
    val cantidadReservas: Int
)

data class ConflictoDto(
    val tipo: String,
    val descripcion: String,
    val reserva: ReservaDto,
    val reservaEnConflicto: ReservaDto?
)

/** Cuerpo de error uniforme del backend ({codigo, mensaje}); en un 409 trae la reserva que choca. */
data class ErrorDto(val codigo: String?, val mensaje: String?, val reservaEnConflicto: ReservaDto?)
