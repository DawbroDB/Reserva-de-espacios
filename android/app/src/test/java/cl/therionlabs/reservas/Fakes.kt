package cl.therionlabs.reservas

import cl.therionlabs.reservas.data.local.ReservaDao
import cl.therionlabs.reservas.data.local.ReservaEntity
import cl.therionlabs.reservas.data.local.SalaDao
import cl.therionlabs.reservas.data.local.SalaEntity
import cl.therionlabs.reservas.data.remote.ReservasApi
import cl.therionlabs.reservas.data.remote.dto.ConflictoDto
import cl.therionlabs.reservas.data.remote.dto.LoginRequestDto
import cl.therionlabs.reservas.data.remote.dto.OcupacionDto
import cl.therionlabs.reservas.data.remote.dto.ReservaDto
import cl.therionlabs.reservas.data.remote.dto.ReservaRequestDto
import cl.therionlabs.reservas.data.remote.dto.SalaDto
import cl.therionlabs.reservas.data.remote.dto.SalaRequestDto
import cl.therionlabs.reservas.data.remote.dto.UsuarioDto
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.notificaciones.ProgramadorRecordatorios
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalTime

/**
 * Fakes escritos a mano (sin Mockito). Funcionan porque los repositorios dependen de las
 * INTERFACES ReservasApi, SalaDao, ReservaDao y ProgramadorRecordatorios.
 */

internal fun salaDto(id: Long, codigo: String, capacidad: Int = 20, proyector: Boolean = true, computadores: Boolean = false) =
    SalaDto(id, codigo, "Sala $codigo", "SALA", capacidad, proyector, computadores, false, true)

internal fun reservaDto(id: Long, salaId: Long, fecha: String, inicio: String, fin: String, usuarioId: Long = 9) =
    ReservaDto(id, salaId, "S-$salaId", "Sala $salaId", usuarioId, "Relator $usuarioId", fecha,
        "$inicio:00", "$fin:00", "Curso $id", "ACTIVA")

/** "Servidor" en memoria que aplica la misma regla anti-topes que el backend real. */
internal class FakeReservasApi : ReservasApi {
    val salas = mutableListOf<SalaDto>()
    val reservas = mutableListOf<ReservaDto>()
    var sinConexion = false
    var llamadasCrear = 0
    var ultimoFiltroDisponibles: List<Any?> = emptyList()

    private fun red() { if (sinConexion) throw IOException("sin red") }

    private fun error(codigo: Int, cuerpo: Any): Nothing =
        throw HttpException(Response.error<Any>(codigo, ResponseBody.create(MediaType.parse("application/json"), Gson().toJson(cuerpo))))

    private fun solapa(r: ReservaDto, salaId: Long, fecha: String, ini: String, fin: String, excluir: Long?) =
        r.estado == "ACTIVA" && r.id != excluir && r.salaId == salaId && r.fecha == fecha &&
            LocalTime.parse(r.horaInicio) < LocalTime.parse(fin) && LocalTime.parse(ini) < LocalTime.parse(r.horaFin)

    override suspend fun login(cuerpo: LoginRequestDto): UsuarioDto {
        red()
        if (cuerpo.password != "demo1234") error(401, mapOf("codigo" to "CREDENCIALES", "mensaje" to "Correo o contraseña incorrectos"))
        return UsuarioDto(1, cuerpo.email, "Relator Test", "RELATOR")
    }

    override suspend fun salas(incluirInactivas: Boolean): List<SalaDto> { red(); return salas.toList() }

    override suspend fun salasDisponibles(
        fecha: String, inicio: String, fin: String, capacidadMin: Int?,
        proyector: Boolean?, computadores: Boolean?, climatizacion: Boolean?, tipo: String?
    ): List<SalaDto> {
        red()
        ultimoFiltroDisponibles = listOf(capacidadMin, proyector, computadores, climatizacion, tipo)
        return salas.filter { s ->
            (capacidadMin == null || s.capacidad >= capacidadMin) && (tipo == null || s.tipo == tipo) &&
                (proyector != true || s.proyector) && (computadores != true || s.computadores) &&
                reservas.none { solapa(it, s.id, fecha, inicio, fin, null) }
        }
    }

    override suspend fun crearSala(cuerpo: SalaRequestDto): SalaDto = TODO()
    override suspend fun actualizarSala(id: Long, cuerpo: SalaRequestDto): SalaDto = TODO()
    override suspend fun desactivarSala(id: Long): Response<Unit> = TODO()

    override suspend fun reservas(fecha: String?, usuarioId: Long?, desde: String?): List<ReservaDto> {
        red()
        return reservas.filter { (fecha == null || it.fecha == fecha) && (usuarioId == null || it.usuarioId == usuarioId) }
    }

    override suspend fun crearReserva(cuerpo: ReservaRequestDto): ReservaDto {
        red()
        llamadasCrear++
        reservas.firstOrNull { solapa(it, cuerpo.salaId, cuerpo.fecha, cuerpo.horaInicio, cuerpo.horaFin, null) }?.let {
            error(409, mapOf("codigo" to "TOPE_HORARIO", "mensaje" to "La sala ya está reservada", "reservaEnConflicto" to it))
        }
        val nueva = ReservaDto(
            (reservas.maxOfOrNull { it.id } ?: 0) + 1, cuerpo.salaId, "S-${cuerpo.salaId}", "Sala ${cuerpo.salaId}",
            cuerpo.usuarioId, "Relator Test", cuerpo.fecha, cuerpo.horaInicio, cuerpo.horaFin, cuerpo.actividad, "ACTIVA"
        )
        reservas += nueva
        return nueva
    }

    override suspend fun actualizarReserva(id: Long, cuerpo: ReservaRequestDto): ReservaDto = TODO()

    override suspend fun cancelarReserva(id: Long): ReservaDto {
        red()
        val i = reservas.indexOfFirst { it.id == id }
        reservas[i] = reservas[i].copy(estado = "CANCELADA")
        return reservas[i]
    }

    override suspend fun conflictos(): List<ConflictoDto> = emptyList()
    override suspend fun ocupacion(desde: String, hasta: String): List<OcupacionDto> = emptyList()
}

internal class FakeSalaDao : SalaDao {
    val filas = MutableStateFlow<List<SalaEntity>>(emptyList())
    override fun observarTodas(): Flow<List<SalaEntity>> = filas.map { l -> l.sortedBy { it.codigo } }
    override suspend fun obtener(id: Long): SalaEntity? = filas.value.firstOrNull { it.id == id }
    override suspend fun guardar(salas: List<SalaEntity>) {
        val ids = salas.map { it.id }.toSet()
        filas.value = filas.value.filterNot { it.id in ids } + salas
    }
    override suspend fun borrarTodas() { filas.value = emptyList() }
}

internal class FakeReservaDao : ReservaDao {
    val filas = MutableStateFlow<List<ReservaEntity>>(emptyList())
    override fun observarDelDia(fecha: String): Flow<List<ReservaEntity>> = filas.map { l -> l.filter { it.fecha == fecha } }
    override fun observarDeUsuario(usuarioId: Long): Flow<List<ReservaEntity>> =
        filas.map { l -> l.filter { it.usuarioId == usuarioId } }
    override suspend fun activasDeSala(salaId: Long, fecha: String): List<ReservaEntity> =
        filas.value.filter { it.salaId == salaId && it.fecha == fecha && it.estado == "ACTIVA" }
    override suspend fun obtener(id: Long): ReservaEntity? = filas.value.firstOrNull { it.id == id }
    override suspend fun guardar(reservas: List<ReservaEntity>) {
        val ids = reservas.map { it.id }.toSet()
        filas.value = filas.value.filterNot { it.id in ids } + reservas
    }
    override suspend fun borrarDelDia(fecha: String) { filas.value = filas.value.filterNot { it.fecha == fecha } }
    override suspend fun borrarDeUsuarioDesde(usuarioId: Long, desde: String) {
        filas.value = filas.value.filterNot { it.usuarioId == usuarioId && it.fecha >= desde }
    }
}

internal class FakeRecordatorios : ProgramadorRecordatorios {
    val programados = mutableListOf<Long>()
    val cancelados = mutableListOf<Long>()
    override fun programar(reserva: Reserva) { programados += reserva.id }
    override fun cancelar(reservaId: Long) { cancelados += reservaId }
}
