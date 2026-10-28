package cl.therionlabs.reservas

import cl.therionlabs.reservas.data.aEntidad
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.repository.TipoError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReservaRepositoryTest {

    private val fecha = LocalDate.of(2030, 1, 8)
    private val api = FakeReservasApi()
    private val dao = FakeReservaDao()
    private val recordatorios = FakeRecordatorios()
    private val repo = ReservaRepository(api, dao, recordatorios)

    private suspend fun crear(inicio: Int, fin: Int) =
        repo.crear(1, 9, fecha, LocalTime.of(inicio, 0), LocalTime.of(fin, 0), "Taller")

    @Test
    fun `crear exitoso guarda en Room y programa el recordatorio`() = runTest {
        val r = crear(10, 12)

        assertTrue(r is ResultadoApi.Exito)
        val id = (r as ResultadoApi.Exito).datos.id
        assertEquals(1, repo.observarDelDia(fecha).first().size)
        assertEquals(listOf(id), recordatorios.programados)
    }

    @Test
    fun `tope del servidor devuelve la reserva en conflicto y refresca la cache del dia`() = runTest {
        api.reservas += reservaDto(50, salaId = 1, fecha = "2030-01-08", inicio = "11:00", fin = "13:00")

        val r = crear(10, 12)

        assertTrue(r is ResultadoApi.Error)
        r as ResultadoApi.Error
        assertEquals(TipoError.TOPE_HORARIO, r.tipo)
        assertEquals(50L, r.reservaEnConflicto?.id)
        assertEquals(LocalTime.of(11, 0), r.reservaEnConflicto?.horaInicio)
        // La reserva ajena ahora está en la caché local para futuros pre-chequeos.
        assertEquals(listOf(50L), repo.observarDelDia(fecha).first().map { it.id })
        assertTrue(recordatorios.programados.isEmpty())
    }

    @Test
    fun `sin conexion la sincronizacion falla pero la cache se conserva`() = runTest {
        dao.guardar(listOf(reservaDto(7, 1, "2030-01-08", "09:00", "10:00").aEntidad()))
        api.sinConexion = true

        val r = repo.sincronizarDia(fecha)

        assertEquals(TipoError.SIN_CONEXION, (r as ResultadoApi.Error).tipo)
        assertEquals(1, repo.observarDelDia(fecha).first().size)
    }

    @Test
    fun `cancelar marca la reserva y elimina su recordatorio`() = runTest {
        val reserva = (crear(10, 11) as ResultadoApi.Exito).datos

        repo.cancelar(reserva)

        assertEquals(listOf(reserva.id), recordatorios.cancelados)
        assertTrue(repo.observarDelDia(fecha).first().none { it.activa })
    }

    @Test
    fun `pre chequeo local detecta solapes pero no bloques contiguos ni la propia reserva`() = runTest {
        dao.guardar(listOf(reservaDto(7, 1, "2030-01-08", "10:00", "12:00").aEntidad()))

        assertNotNull(repo.buscarTopeLocal(1, fecha, LocalTime.of(11, 0), LocalTime.of(13, 0)))
        assertNull(repo.buscarTopeLocal(1, fecha, LocalTime.of(12, 0), LocalTime.of(13, 0)))
        assertNull(repo.buscarTopeLocal(2, fecha, LocalTime.of(10, 0), LocalTime.of(12, 0)))
        assertNull(repo.buscarTopeLocal(1, fecha, LocalTime.of(10, 30), LocalTime.of(11, 0), excluirId = 7))
    }
}
