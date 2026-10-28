package cl.therionlabs.reservas

import cl.therionlabs.reservas.data.aEntidad
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.viewmodel.MisReservasViewModel
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MisReservasViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val ahora = LocalDateTime.of(2030, 1, 7, 12, 0)
    private val dao = FakeReservaDao()

    @Before
    fun configurar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private suspend fun cargar(vararg reservas: Pair<Long, String>) =
        dao.guardar(reservas.map { (id, fechaHora) ->
            val (fecha, inicio) = fechaHora.split(" ")
            reservaDto(id, salaId = 1, fecha = fecha, inicio = inicio, fin = "${inicio.take(2).toInt() + 1}:00", usuarioId = 9).aEntidad()
        })

    private fun viewModel() =
        MisReservasViewModel(ReservaRepository(FakeReservasApi(), dao, FakeRecordatorios()), 9, { ahora })

    @Test
    fun `ordena de la mas proxima a la mas lejana`() = runTest(dispatcher) {
        cargar(
            1L to "2030-01-20 10:00", 2L to "2030-01-08 15:00", 3L to "2030-01-08 09:00",
            4L to "2030-01-02 10:00", 5L to "2030-01-06 10:00"
        )
        val vm = viewModel()
        backgroundScope.launch { vm.lista.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(3L, 2L, 1L), vm.lista.value.proximas.map { it.id })
        assertEquals(listOf(5L, 4L), vm.lista.value.pasadas.map { it.id })
    }

    @Test
    fun `filtra por rango de fechas inclusivo`() = runTest(dispatcher) {
        cargar(1L to "2030-01-08 10:00", 2L to "2030-01-10 10:00", 3L to "2030-01-15 10:00")
        val vm = viewModel()
        backgroundScope.launch { vm.lista.collect {} }

        vm.activarFiltro()
        vm.cambiarDesde(LocalDate.of(2030, 1, 10))
        vm.cambiarHasta(LocalDate.of(2030, 1, 15))
        advanceUntilIdle()
        assertEquals(listOf(2L, 3L), vm.lista.value.proximas.map { it.id })

        // Un "hasta" anterior al "desde" arrastra el inicio del rango.
        vm.cambiarHasta(LocalDate.of(2030, 1, 8))
        advanceUntilIdle()
        assertEquals(LocalDate.of(2030, 1, 8), vm.filtro.value?.desde)
        assertEquals(listOf(1L), vm.lista.value.proximas.map { it.id })

        vm.quitarFiltro()
        advanceUntilIdle()
        assertEquals(3, vm.lista.value.proximas.size)
    }
}
