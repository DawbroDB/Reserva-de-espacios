package cl.therionlabs.reservas

import cl.therionlabs.reservas.data.aModelo
import cl.therionlabs.reservas.model.BloqueHorario
import cl.therionlabs.reservas.model.FilaAgenda
import cl.therionlabs.reservas.model.TipoSala
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.repository.SalaRepository
import cl.therionlabs.reservas.viewmodel.BuscarSalasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class BuscarSalasViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val hoy = LocalDate.of(2030, 1, 7)
    private val api = FakeReservasApi().apply {
        salas += salaDto(1, "SC-101", capacidad = 30, proyector = true)
        salas += salaDto(2, "LAB-01", capacidad = 24, proyector = true, computadores = true)
        salas += salaDto(3, "SC-103", capacidad = 12, proyector = false)
        reservas += reservaDto(1, salaId = 2, fecha = "2030-01-07", inicio = "10:00", fin = "12:00")
    }

    @Before
    fun configurar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun viewModel() = BuscarSalasViewModel(SalaRepository(api, FakeSalaDao()), { hoy }, { LocalTime.of(9, 20) })

    @Test
    fun `propone el siguiente bloque completo`() {
        val e = viewModel().estado.value
        assertEquals(LocalTime.of(10, 0), e.inicio)
        assertEquals(LocalTime.of(11, 0), e.fin)
        assertEquals(hoy, e.fecha)
        assertEquals(hoy.plusDays(1), BuscarSalasViewModel.estadoInicial(hoy, LocalTime.of(21, 30)).fecha)
        assertEquals(BloqueHorario.APERTURA, BuscarSalasViewModel.estadoInicial(hoy, LocalTime.of(6, 0)).inicio)
    }

    @Test
    fun `filtra por capacidad y equipamiento y excluye salas ocupadas`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.alternarProyector()
        vm.cambiarCapacidad("20")

        vm.buscar()
        advanceUntilIdle()

        // LAB-01 cumple filtros pero está ocupada de 10 a 12; SC-103 no tiene proyector.
        val salas = (vm.resultado.value as ResultadoApi.Exito).datos
        assertEquals(listOf("SC-101"), salas.map { it.codigo })
        assertEquals(listOf(20, true, null, null, null), api.ultimoFiltroDisponibles)
    }

    @Test
    fun `filtra por tipo de sala`() = runTest(dispatcher) {
        api.salas += salaDto(4, "LAB-02").copy(tipo = "LABORATORIO")
        val vm = viewModel()
        vm.cambiarTipo(TipoSala.LABORATORIO)

        vm.buscar()
        advanceUntilIdle()

        // Las demás salas del fake son de tipo SALA.
        assertEquals(listOf("LAB-02"), (vm.resultado.value as ResultadoApi.Exito).datos.map { it.codigo })
        assertEquals("LABORATORIO", api.ultimoFiltroDisponibles.last())
    }

    @Test
    fun `rango invalido no consulta el servidor`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.cambiarFin(LocalTime.of(9, 0))

        vm.buscar()
        advanceUntilIdle()

        assertNotNull(vm.estado.value.error)
        assertNull(vm.resultado.value)
    }

    @Test
    fun `una reserva de 10 a 11 30 ocupa los bloques de las 10 y las 11 en la agenda`() {
        val sala = api.salas[0].aModelo()
        val reserva = reservaDto(9, salaId = 1, fecha = "2030-01-07", inicio = "10:00", fin = "11:30").aModelo()

        val fila = FilaAgenda.construir(sala, listOf(reserva))

        val ocupados = fila.celdas.filterNot { it.libre }.map { it.bloque.inicio.hour }
        assertEquals(listOf(10, 11), ocupados)
        assertEquals(BloqueHorario.DEL_DIA.size - 2, fila.bloquesLibres)
    }
}
