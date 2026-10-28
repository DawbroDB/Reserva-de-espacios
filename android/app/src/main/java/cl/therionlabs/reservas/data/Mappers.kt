package cl.therionlabs.reservas.data

import cl.therionlabs.reservas.data.local.ReservaEntity
import cl.therionlabs.reservas.data.local.SalaEntity
import cl.therionlabs.reservas.data.remote.dto.ConflictoDto
import cl.therionlabs.reservas.data.remote.dto.OcupacionDto
import cl.therionlabs.reservas.data.remote.dto.ReservaDto
import cl.therionlabs.reservas.data.remote.dto.SalaDto
import cl.therionlabs.reservas.data.remote.dto.SalaRequestDto
import cl.therionlabs.reservas.data.remote.dto.UsuarioDto
import cl.therionlabs.reservas.model.Conflicto
import cl.therionlabs.reservas.model.EstadoReserva
import cl.therionlabs.reservas.model.Ocupacion
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.Rol
import cl.therionlabs.reservas.model.Sala
import cl.therionlabs.reservas.model.TipoSala
import cl.therionlabs.reservas.model.Usuario
import java.time.LocalTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Conversión DTO (red) ↔ Entity (Room) ↔ modelo de dominio. */

private val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.aTexto(): String = format(HORA)
fun LocalDate.aTexto(): String = toString()

/** El backend envía "14:00:00"; LocalTime.parse acepta con o sin segundos. */
private fun hora(texto: String): LocalTime = LocalTime.parse(texto)

private fun tipoSala(texto: String): TipoSala =
    TipoSala.entries.firstOrNull { it.name == texto } ?: TipoSala.SALA

private fun estado(texto: String): EstadoReserva =
    EstadoReserva.entries.firstOrNull { it.name == texto } ?: EstadoReserva.ACTIVA

fun UsuarioDto.aModelo() = Usuario(id, email, nombre, if (rol == "COORDINADOR") Rol.COORDINADOR else Rol.RELATOR)

fun SalaDto.aEntidad() = SalaEntity(id, codigo, nombre, tipo, capacidad, proyector, computadores, climatizacion, activa)
fun SalaDto.aModelo() = aEntidad().aModelo()
fun SalaEntity.aModelo() =
    Sala(id, codigo, nombre, tipoSala(tipo), capacidad, proyector, computadores, climatizacion, activa)

fun Sala.aRequest() =
    SalaRequestDto(codigo, nombre, tipo.name, capacidad, proyector, computadores, climatizacion, activa)

fun ReservaDto.aEntidad() = ReservaEntity(
    id = id,
    salaId = salaId,
    salaCodigo = salaCodigo,
    salaNombre = salaNombre,
    usuarioId = usuarioId,
    relator = relator,
    fecha = fecha,
    horaInicio = hora(horaInicio).aTexto(),
    horaFin = hora(horaFin).aTexto(),
    actividad = actividad,
    estado = estado
)

fun ReservaDto.aModelo() = aEntidad().aModelo()

fun ReservaEntity.aModelo() = Reserva(
    id = id,
    salaId = salaId,
    salaCodigo = salaCodigo,
    salaNombre = salaNombre,
    usuarioId = usuarioId,
    relator = relator,
    fecha = LocalDate.parse(fecha),
    horaInicio = hora(horaInicio),
    horaFin = hora(horaFin),
    actividad = actividad,
    estado = estado(estado)
)

fun OcupacionDto.aModelo() =
    Ocupacion(salaId, salaCodigo, salaNombre, minutosReservados, minutosDisponibles, porcentaje, cantidadReservas)

fun ConflictoDto.aModelo() = Conflicto(tipo, descripcion, reserva.aModelo(), reservaEnConflicto?.aModelo())
