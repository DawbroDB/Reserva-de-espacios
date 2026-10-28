package cl.therionlabs.reservas.viewmodel

import cl.therionlabs.reservas.model.Reloj
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.therionlabs.reservas.data.ServiceLocator
import cl.therionlabs.reservas.model.FilaAgenda
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.repository.SalaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Agenda diaria: grilla salas × bloques horarios. Combina dos Flows de Room (salas y reservas
 * del día elegido), así la grilla se redibuja sola cuando llega una sincronización.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModel(
    private val salas: SalaRepository,
    private val reservas: ReservaRepository,
    private val hoy: () -> LocalDate = Reloj::hoy
) : ViewModel() {

    private val _fecha = MutableStateFlow(hoy())
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()

    val filas: StateFlow<List<FilaAgenda>> = combine(
        salas.observarSalas(),
        _fecha.flatMapLatest { reservas.observarDelDia(it) }
    ) { listaSalas, reservasDelDia ->
        listaSalas.filter { it.activa }.map { FilaAgenda.construir(it, reservasDelDia) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    /** Mensaje de "sin conexión" mientras se muestran datos de la caché. */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    fun cambiarFecha(nueva: LocalDate) {
        if (nueva == _fecha.value) return
        _fecha.value = nueva
        refrescar()
    }

    fun diaSiguiente() = cambiarFecha(_fecha.value.plusDays(1))
    fun diaAnterior() = cambiarFecha(_fecha.value.minusDays(1))
    fun irAHoy() = cambiarFecha(hoy())

    /** La pantalla lo llama al abrirse y cada 30 s mientras está visible ("tiempo real" del MVP). */
    fun refrescar(mostrarIndicador: Boolean = false) {
        viewModelScope.launch {
            if (mostrarIndicador) _sincronizando.value = true
            val rSalas = salas.sincronizar()
            val rReservas = reservas.sincronizarDia(_fecha.value)
            _aviso.value = (rReservas as? ResultadoApi.Error ?: rSalas as? ResultadoApi.Error)?.mensaje
            _sincronizando.value = false
        }
    }

    /** Solo el coordinador: cancela la reserva de cualquier relator; la grilla libera el bloque vía Room. */
    fun cancelar(reserva: Reserva) {
        viewModelScope.launch {
            when (val r = reservas.cancelar(reserva)) {
                is ResultadoApi.Exito -> _mensajes.emit("Se canceló \"${reserva.actividad}\" (${reserva.relator})")
                is ResultadoApi.Error -> _mensajes.emit(r.mensaje)
                ResultadoApi.Cargando -> Unit
            }
        }
    }

    companion object {
        fun fabrica(contexto: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = ServiceLocator.obtener(contexto)
                AgendaViewModel(c.salas, c.reservas)
            }
        }
    }
}
