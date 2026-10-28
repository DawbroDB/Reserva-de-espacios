package cl.therionlabs.reservas.viewmodel

import cl.therionlabs.reservas.model.Reloj
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.therionlabs.reservas.data.ServiceLocator
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.ResultadoApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class MisReservas(val proximas: List<Reserva> = emptyList(), val pasadas: List<Reserva> = emptyList())

/** Rango de fechas inclusivo para filtrar la lista. */
data class RangoFechas(val desde: LocalDate, val hasta: LocalDate) {
    fun contiene(fecha: LocalDate): Boolean = fecha in desde..hasta
}

class MisReservasViewModel(
    private val reservas: ReservaRepository,
    private val usuarioId: Long,
    private val ahora: () -> LocalDateTime = Reloj::ahora
) : ViewModel() {

    /** null = sin filtro (todas las fechas). */
    private val _filtro = MutableStateFlow<RangoFechas?>(null)
    val filtro: StateFlow<RangoFechas?> = _filtro.asStateFlow()

    /** Ambas listas van de la más cercana a hoy a la más lejana. */
    val lista: StateFlow<MisReservas> = combine(reservas.observarDeUsuario(usuarioId), _filtro) { todas, rango ->
        val momento = ahora()
        val (proximas, pasadas) = todas
            .filter { it.activa && (rango == null || rango.contiene(it.fecha)) }
            .partition { LocalDateTime.of(it.fecha, it.horaFin) > momento }
        MisReservas(
            proximas.sortedBy { LocalDateTime.of(it.fecha, it.horaInicio) },
            pasadas.sortedByDescending { LocalDateTime.of(it.fecha, it.horaInicio) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MisReservas())

    /** Activa el filtro con la próxima semana como rango inicial. */
    fun activarFiltro() {
        val hoy = ahora().toLocalDate()
        _filtro.value = RangoFechas(hoy, hoy.plusDays(6))
    }

    fun quitarFiltro() { _filtro.value = null }

    /** Si el rango quedara invertido, se arrastra el otro extremo. */
    fun cambiarDesde(fecha: LocalDate) = _filtro.update { it?.copy(desde = fecha, hasta = maxOf(it.hasta, fecha)) }
    fun cambiarHasta(fecha: LocalDate) = _filtro.update { it?.copy(desde = minOf(it.desde, fecha), hasta = fecha) }

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    fun refrescar() {
        viewModelScope.launch {
            _sincronizando.value = true
            val r = reservas.sincronizarDeUsuario(usuarioId, desde = ahora().toLocalDate().minusDays(14))
            if (r is ResultadoApi.Error) _mensajes.emit(r.mensaje)
            _sincronizando.value = false
        }
    }

    fun cancelar(reserva: Reserva) {
        viewModelScope.launch {
            when (val r = reservas.cancelar(reserva)) {
                is ResultadoApi.Exito -> _mensajes.emit("Reserva \"${reserva.actividad}\" cancelada")
                is ResultadoApi.Error -> _mensajes.emit(r.mensaje)
                ResultadoApi.Cargando -> Unit
            }
        }
    }

    companion object {
        fun fabrica(contexto: Context, usuarioId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { MisReservasViewModel(ServiceLocator.obtener(contexto).reservas, usuarioId) }
        }
    }
}
