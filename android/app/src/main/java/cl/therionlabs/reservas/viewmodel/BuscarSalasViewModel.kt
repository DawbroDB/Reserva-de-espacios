package cl.therionlabs.reservas.viewmodel

import cl.therionlabs.reservas.model.Reloj
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.therionlabs.reservas.data.ServiceLocator
import cl.therionlabs.reservas.model.BloqueHorario
import cl.therionlabs.reservas.model.FiltroSalas
import cl.therionlabs.reservas.model.Sala
import cl.therionlabs.reservas.model.TipoSala
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.repository.SalaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class BusquedaUiState(
    val fecha: LocalDate,
    val inicio: LocalTime,
    val fin: LocalTime,
    val capacidad: String = "",
    val proyector: Boolean = false,
    val computadores: Boolean = false,
    val climatizacion: Boolean = false,
    /** null = todos los tipos. */
    val tipo: TipoSala? = null,
    val error: String? = null
) {
    val filtro: FiltroSalas
        get() = FiltroSalas(capacidad.toIntOrNull(), proyector, computadores, climatizacion, tipo)
}

/** Búsqueda de salas libres en un rango horario, filtrando por tipo, capacidad y equipamiento. */
class BuscarSalasViewModel(
    private val salas: SalaRepository,
    hoy: () -> LocalDate = Reloj::hoy,
    ahora: () -> LocalTime = Reloj::hora
) : ViewModel() {

    private val _estado = MutableStateFlow(estadoInicial(hoy(), ahora()))
    val estado: StateFlow<BusquedaUiState> = _estado.asStateFlow()

    /** null = todavía no se ha buscado. */
    private val _resultado = MutableStateFlow<ResultadoApi<List<Sala>>?>(null)
    val resultado: StateFlow<ResultadoApi<List<Sala>>?> = _resultado.asStateFlow()

    fun cambiarFecha(valor: LocalDate) = _estado.update { it.copy(fecha = valor, error = null) }
    fun cambiarInicio(valor: LocalTime) = _estado.update {
        // Mantiene la duración razonable: si el inicio pasa al fin, se corre el fin una hora.
        val fin = if (valor < it.fin) it.fin else minOf(valor.plusHours(1), BloqueHorario.CIERRE)
        it.copy(inicio = valor, fin = fin, error = null)
    }
    fun cambiarFin(valor: LocalTime) = _estado.update { it.copy(fin = valor, error = null) }
    fun cambiarCapacidad(valor: String) = _estado.update {
        it.copy(capacidad = valor.filter(Char::isDigit).take(3), error = null)
    }
    fun alternarProyector() = _estado.update { it.copy(proyector = !it.proyector) }
    fun alternarComputadores() = _estado.update { it.copy(computadores = !it.computadores) }
    fun alternarClimatizacion() = _estado.update { it.copy(climatizacion = !it.climatizacion) }
    fun cambiarTipo(valor: TipoSala?) = _estado.update { it.copy(tipo = valor) }

    fun buscar() {
        val e = _estado.value
        val error = validarRango(e.inicio, e.fin)
        if (error != null) {
            _estado.update { it.copy(error = error) }
            return
        }
        _resultado.value = ResultadoApi.Cargando
        viewModelScope.launch {
            _resultado.value = salas.disponibles(e.fecha, e.inicio, e.fin, e.filtro)
        }
    }

    companion object {
        /** Propone el próximo bloque completo de una hora a partir de ahora. */
        internal fun estadoInicial(hoy: LocalDate, ahora: LocalTime): BusquedaUiState {
            val apertura = BloqueHorario.APERTURA
            // Pasadas las 21:00 ya no cabe un bloque hoy: se propone mañana a primera hora.
            if (ahora.hour + 1 >= BloqueHorario.CIERRE.hour) {
                return BusquedaUiState(hoy.plusDays(1), apertura, apertura.plusHours(1))
            }
            val inicio = maxOf(LocalTime.of(ahora.hour + 1, 0), apertura)
            return BusquedaUiState(hoy, inicio, inicio.plusHours(1))
        }

        fun fabrica(contexto: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { BuscarSalasViewModel(ServiceLocator.obtener(contexto).salas) }
        }
    }
}

/** Reglas de horario compartidas por la búsqueda y el formulario de reserva. */
internal fun validarRango(inicio: LocalTime, fin: LocalTime): String? = when {
    inicio >= fin -> "La hora de término debe ser posterior a la de inicio"
    inicio < BloqueHorario.APERTURA || fin > BloqueHorario.CIERRE ->
        "El horario de la sede es de ${BloqueHorario.APERTURA} a ${BloqueHorario.CIERRE}"
    else -> null
}
