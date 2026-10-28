package cl.therionlabs.reservas

import cl.therionlabs.reservas.data.aEntidad
import cl.therionlabs.reservas.data.aModelo
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.SalaRepository
import cl.therionlabs.reservas.viewmodel.AgendaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val hoy = LocalDate.of(2030, 1, 7)
    private val api = FakeReservasApi().apply {
        salas += salaDto(1, "SC-101")
        reservas += reservaDto(5, salaId = 1, fecha = "2030-01-07", inicio = "10:00", fin = "12:00", usuarioId = 3)
    }
    private val salaDao = FakeSalaDao()
    private val reservaDao = FakeReservaDao()

    @Before
    fun configurar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `el coordinador cancela una reserva ajena y el bloque queda libre`() = runTest(dispatcher) {
        salaDao.guardar(api.salas.map { it.aEntidad() })
        reservaDao.guardar(api.reservas.map { it.aEntidad() })
        val vm = AgendaViewModel(
            SalaRepository(api, salaDao),
            ReservaRepository(api, reservaDao, FakeRecordatorios()),
            { hoy }
        )
        backgroundScope.launch { vm.filas.collect {} }
        val mensajes = mutableListOf<String>()
        backgroundScope.launch { vm.mensajes.collect { mensajes += it } }
        advanceUntilIdle()
        assertEquals(2, vm.filas.value.single().celdas.count { !it.libre })

        vm.cancelar(api.reservas.single().aModelo())
        advanceUntilIdle()

        assertEquals("CANCELADA", api.reservas.single().estado)
        assertEquals(0, vm.filas.value.single().celdas.count { !it.libre })
        assertEquals(1, mensajes.size)
    }
}
