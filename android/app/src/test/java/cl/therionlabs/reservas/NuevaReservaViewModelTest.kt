package cl.therionlabs.reservas

import cl.therionlabs.reservas.data.aEntidad
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.SalaRepository
import cl.therionlabs.reservas.viewmodel.ArgumentosReserva
import cl.therionlabs.reservas.viewmodel.EventoReserva
import cl.therionlabs.reservas.viewmodel.NuevaReservaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Dispatchers.setMain reemplaza el hilo principal de Android para que viewModelScope
 * funcione en la JVM. "Ahora" es fijo: lunes 7 de enero de 2030, 08:00.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NuevaReservaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val manana = LocalDate.of(2030, 1, 8)
    private val api = FakeReservasApi().apply { salas += salaDto(1, "LAB-01") }
    private val salaDao = FakeSalaDao()
    private val reservaDao = FakeReservaDao()

    @Before
    fun configurar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun viewModel(fecha: LocalDate = manana, inicio: Int = 10, fin: Int = 12): NuevaReservaViewModel {
        val reservas = ReservaRepository(api, reservaDao, FakeRecordatorios())
        return NuevaReservaViewModel(
            SalaRepository(api, salaDao), reservas, usuarioId = 9,
            args = ArgumentosReserva(1, fecha, LocalTime.of(inicio, 0), LocalTime.of(fin, 0)),
            ahora = { LocalDateTime.of(2030, 1, 7, 8, 0) }
        )
    }

    @Test
    fun `actividad vacia muestra error y no llama al servidor`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.guardar()
        advanceUntilIdle()

        assertNotNull(vm.estado.value.errorActividad)
        assertEquals(0, api.llamadasCrear)
    }

    @Test
    fun `termino antes del inicio o fuera de horario es invalido`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.cambiarActividad("Excel Avanzado")
        vm.cambiarFin(LocalTime.of(9, 0))
        assertTrue(!vm.validar())
        assertNotNull(vm.estado.value.errorHorario)

        vm.cambiarInicio(LocalTime.of(21, 0))
        vm.cambiarFin(LocalTime.of(23, 0))
        assertTrue(!vm.validar())
    }

    @Test
    fun `no permite reservar en el pasado`() = runTest(dispatcher) {
        val vm = viewModel(fecha = LocalDate.of(2030, 1, 6))
        vm.cambiarActividad("Excel Avanzado")
        assertTrue(!vm.validar())
        assertEquals("No puedes reservar en el pasado", vm.estado.value.errorHorario)
    }

    @Test
    fun `tope en la cache local se informa sin llamar al servidor`() = runTest(dispatcher) {
        reservaDao.guardar(listOf(reservaDto(5, 1, "2030-01-08", "11:00", "12:00").aEntidad()))
        // La sincronización al abrir traerá lo mismo que hay en el "servidor".
        api.reservas += reservaDto(5, 1, "2030-01-08", "11:00", "12:00")
        val vm = viewModel()
        advanceUntilIdle()
        val llamadasPrevias = api.llamadasCrear

        vm.cambiarActividad("Excel Avanzado")
        vm.guardar()
        advanceUntilIdle()

        assertEquals(5L, vm.estado.value.conflicto?.id)
        assertEquals(llamadasPrevias, api.llamadasCrear)
    }

    @Test
    fun `reserva valida emite Guardada`() = runTest(dispatcher) {
        val vm = viewModel()
        val evento = async { vm.eventos.first() }
        advanceUntilIdle()

        vm.cambiarActividad("Python para Datos")
        vm.guardar()
        advanceUntilIdle()

        val e = evento.await()
        assertTrue("evento=$e", e is EventoReserva.Guardada)
        assertEquals("Python para Datos", (e as EventoReserva.Guardada).reserva.actividad)
    }

    @Test
    fun `tope informado por el servidor se muestra en el formulario`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        // Otro relator reservó después de que abrimos el formulario: la caché local no lo sabe.
        api.reservas += reservaDto(77, 1, "2030-01-08", "09:00", "11:00")

        vm.cambiarActividad("Excel Avanzado")
        vm.guardar()
        advanceUntilIdle()

        assertEquals("La sala ya está reservada", vm.estado.value.errorHorario)
        assertEquals(77L, vm.estado.value.conflicto?.id)
    }
}
