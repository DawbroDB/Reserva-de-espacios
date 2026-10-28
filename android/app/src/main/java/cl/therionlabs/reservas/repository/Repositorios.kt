package cl.therionlabs.reservas.repository

import cl.therionlabs.reservas.data.AlmacenSesion
import cl.therionlabs.reservas.data.aEntidad
import cl.therionlabs.reservas.data.aModelo
import cl.therionlabs.reservas.data.aRequest
import cl.therionlabs.reservas.data.aTexto
import cl.therionlabs.reservas.data.local.ReservaDao
import cl.therionlabs.reservas.data.local.SalaDao
import cl.therionlabs.reservas.data.remote.ReservasApi
import cl.therionlabs.reservas.data.remote.dto.LoginRequestDto
import cl.therionlabs.reservas.data.remote.dto.ReservaRequestDto
import cl.therionlabs.reservas.model.Conflicto
import cl.therionlabs.reservas.model.FiltroSalas
import cl.therionlabs.reservas.model.Ocupacion
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.Sala
import cl.therionlabs.reservas.model.Usuario
import cl.therionlabs.reservas.notificaciones.ProgramadorRecordatorios
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * Los repositorios son la única puerta entre la UI y los datos: las pantallas nunca importan
 * Retrofit ni Room. Estrategia offline-first: la UI observa Room y `sincronizar*` lo actualiza
 * desde el servidor. Crear o modificar reservas siempre pasa por el servidor, que es quien
 * garantiza que no existan topes.
 */

class AuthRepository(
    private val api: ReservasApi,
    private val sesion: AlmacenSesion
) {
    fun usuarioActual(): Usuario? = sesion.leer()

    suspend fun login(email: String, password: String): ResultadoApi<Usuario> {
        val resultado = llamar { api.login(LoginRequestDto(email.trim(), password)).aModelo() }
        if (resultado is ResultadoApi.Exito) sesion.guardar(resultado.datos)
        return resultado
    }

    fun cerrarSesion() = sesion.limpiar()
}

class SalaRepository(
    private val api: ReservasApi,
    private val dao: SalaDao
) {
    fun observarSalas(): Flow<List<Sala>> = dao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    suspend fun obtener(id: Long): Sala? = dao.obtener(id)?.aModelo()

    suspend fun sincronizar(): ResultadoApi<Unit> = llamar {
        dao.reemplazarTodas(api.salas(incluirInactivas = true).map { it.aEntidad() })
    }

    suspend fun disponibles(
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        filtro: FiltroSalas
    ): ResultadoApi<List<Sala>> = llamar {
        api.salasDisponibles(
            fecha = fecha.aTexto(),
            inicio = inicio.aTexto(),
            fin = fin.aTexto(),
            capacidadMin = filtro.capacidadMin,
            proyector = filtro.proyector.takeIf { it },
            computadores = filtro.computadores.takeIf { it },
            climatizacion = filtro.climatizacion.takeIf { it },
            tipo = filtro.tipo?.name
        ).map { it.aModelo() }
    }

    /** id = 0 crea una sala nueva; otro id actualiza la existente. */
    suspend fun guardar(sala: Sala): ResultadoApi<Sala> = llamar {
        val dto = if (sala.id == 0L) api.crearSala(sala.aRequest()) else api.actualizarSala(sala.id, sala.aRequest())
        dao.guardar(listOf(dto.aEntidad()))
        dto.aModelo()
    }

    suspend fun desactivar(id: Long): ResultadoApi<Unit> {
        val resultado = llamar {
            val respuesta = api.desactivarSala(id)
            if (!respuesta.isSuccessful) throw retrofit2.HttpException(respuesta)
        }
        if (resultado is ResultadoApi.Exito) sincronizar()
        return resultado
    }
}

class ReservaRepository(
    private val api: ReservasApi,
    private val dao: ReservaDao,
    private val recordatorios: ProgramadorRecordatorios
) {
    fun observarDelDia(fecha: LocalDate): Flow<List<Reserva>> =
        dao.observarDelDia(fecha.aTexto()).map { lista -> lista.map { it.aModelo() } }

    fun observarDeUsuario(usuarioId: Long): Flow<List<Reserva>> =
        dao.observarDeUsuario(usuarioId).map { lista -> lista.map { it.aModelo() } }

    suspend fun sincronizarDia(fecha: LocalDate): ResultadoApi<Unit> = llamar {
        dao.reemplazarDelDia(fecha.aTexto(), api.reservas(fecha = fecha.aTexto()).map { it.aEntidad() })
    }

    /** Trae las reservas futuras del usuario y deja sus recordatorios al día (sirve tras reinstalar). */
    suspend fun sincronizarDeUsuario(usuarioId: Long, desde: LocalDate): ResultadoApi<Unit> = llamar {
        val reservas = api.reservas(usuarioId = usuarioId, desde = desde.aTexto())
        dao.reemplazarDeUsuario(usuarioId, desde.aTexto(), reservas.map { it.aEntidad() })
        reservas.map { it.aModelo() }.forEach { recordatorios.programar(it) }
    }

    /**
     * Pre-chequeo contra la caché local para avisar de inmediato sin esperar la red.
     * No reemplaza la validación del servidor: la caché puede estar desactualizada.
     */
    suspend fun buscarTopeLocal(
        salaId: Long,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        excluirId: Long? = null
    ): Reserva? = dao.activasDeSala(salaId, fecha.aTexto())
        .map { it.aModelo() }
        .firstOrNull { it.id != excluirId && it.seSolapaCon(inicio, fin) }

    suspend fun crear(
        salaId: Long,
        usuarioId: Long,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        actividad: String
    ): ResultadoApi<Reserva> = guardarEnServidor(null, ReservaRequestDto(
        salaId, usuarioId, fecha.aTexto(), inicio.aTexto(), fin.aTexto(), actividad.trim()
    ))

    suspend fun actualizar(
        reservaId: Long,
        salaId: Long,
        usuarioId: Long,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        actividad: String
    ): ResultadoApi<Reserva> = guardarEnServidor(reservaId, ReservaRequestDto(
        salaId, usuarioId, fecha.aTexto(), inicio.aTexto(), fin.aTexto(), actividad.trim()
    ))

    private suspend fun guardarEnServidor(id: Long?, cuerpo: ReservaRequestDto): ResultadoApi<Reserva> {
        val resultado = llamar {
            val dto = if (id == null) api.crearReserva(cuerpo) else api.actualizarReserva(id, cuerpo)
            dao.guardar(listOf(dto.aEntidad()))
            dto.aModelo().also { recordatorios.programar(it) }
        }
        // Si hubo tope, la agenda local estaba vieja: se refresca ese día.
        if (resultado is ResultadoApi.Error && resultado.tipo == TipoError.TOPE_HORARIO) {
            sincronizarDia(LocalDate.parse(cuerpo.fecha))
        }
        return resultado
    }

    suspend fun obtener(id: Long): Reserva? = dao.obtener(id)?.aModelo()

    suspend fun cancelar(reserva: Reserva): ResultadoApi<Reserva> = llamar {
        val dto = api.cancelarReserva(reserva.id)
        dao.guardar(listOf(dto.aEntidad()))
        recordatorios.cancelar(reserva.id)
        dto.aModelo()
    }

    suspend fun conflictos(): ResultadoApi<List<Conflicto>> = llamar { api.conflictos().map { it.aModelo() } }

    suspend fun ocupacion(desde: LocalDate, hasta: LocalDate): ResultadoApi<List<Ocupacion>> =
        llamar { api.ocupacion(desde.aTexto(), hasta.aTexto()).map { it.aModelo() } }
}
