package cl.therionlabs.reservas.viewmodel

import cl.therionlabs.reservas.model.Reloj
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.therionlabs.reservas.data.ServiceLocator
import cl.therionlabs.reservas.model.Conflicto
import cl.therionlabs.reservas.model.Ocupacion
import cl.therionlabs.reservas.model.Reserva
import cl.therionlabs.reservas.model.Sala
import cl.therionlabs.reservas.model.TipoSala
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.repository.SalaRepository
import cl.therionlabs.reservas.repository.mapear
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Catálogo completo (incluye salas fuera de servicio) para el Coordinador de Infraestructura. */
class CatalogoSalasViewModel(private val salas: SalaRepository) : ViewModel() {

    val lista: StateFlow<List<Sala>> = salas.observarSalas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    fun refrescar() {
        viewModelScope.launch { _aviso.value = (salas.sincronizar() as? ResultadoApi.Error)?.mensaje }
    }

    companion object {
        fun fabrica(contexto: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { CatalogoSalasViewModel(ServiceLocator.obtener(contexto).salas) }
        }
    }
}

data class SalaFormUiState(
    val id: Long = 0,
    val codigo: String = "",
    val nombre: String = "",
    val tipo: TipoSala = TipoSala.SALA,
    val capacidad: String = "",
    val proyector: Boolean = false,
    val computadores: Boolean = false,
    val climatizacion: Boolean = false,
    val activa: Boolean = true,
    val errores: Map<String, String> = emptyMap(),
    val enviando: Boolean = false
) {
    val esNueva: Boolean get() = id == 0L
}

sealed interface EventoFormulario {
    data object Listo : EventoFormulario
    data class Fallo(val mensaje: String) : EventoFormulario
}

class SalaFormViewModel(private val salas: SalaRepository, salaId: Long?) : ViewModel() {

    private val _estado = MutableStateFlow(SalaFormUiState())
    val estado: StateFlow<SalaFormUiState> = _estado.asStateFlow()

    /** Listo = guardado o desactivado con éxito (volver al catálogo). */
    private val _eventos = MutableSharedFlow<EventoFormulario>(extraBufferCapacity = 1)
    val eventos: SharedFlow<EventoFormulario> = _eventos.asSharedFlow()

    init {
        if (salaId != null) {
            viewModelScope.launch {
                salas.obtener(salaId)?.let { s ->
                    _estado.value = SalaFormUiState(
                        s.id, s.codigo, s.nombre, s.tipo, s.capacidad.toString(),
                        s.proyector, s.computadores, s.climatizacion, s.activa
                    )
                }
            }
        }
    }

    fun actualizar(cambio: SalaFormUiState.() -> SalaFormUiState) =
        _estado.update { it.cambio().copy(errores = emptyMap()) }

    internal fun validar(): Boolean {
        val e = _estado.value
        val errores = buildMap {
            if (!Regex("^[A-Za-z0-9-]{2,20}$").matches(e.codigo.trim())) put("codigo", "Usa 2 a 20 letras, números o guiones")
            if (e.nombre.isBlank()) put("nombre", "Ingresa un nombre")
            val cap = e.capacidad.toIntOrNull()
            if (cap == null || cap !in 1..500) put("capacidad", "Capacidad entre 1 y 500")
        }
        _estado.update { it.copy(errores = errores) }
        return errores.isEmpty()
    }

    fun guardar() {
        if (_estado.value.enviando || !validar()) return
        val e = _estado.value
        _estado.update { it.copy(enviando = true) }
        viewModelScope.launch {
            val sala = Sala(
                e.id, e.codigo.trim().uppercase(), e.nombre.trim(), e.tipo, e.capacidad.toInt(),
                e.proyector, e.computadores, e.climatizacion, e.activa
            )
            val r = salas.guardar(sala)
            _estado.update { it.copy(enviando = false) }
            _eventos.emit(if (r is ResultadoApi.Error) EventoFormulario.Fallo(r.mensaje) else EventoFormulario.Listo)
        }
    }

    fun desactivar() {
        val id = _estado.value.id
        if (id == 0L) return
        viewModelScope.launch {
            val r = salas.desactivar(id)
            _eventos.emit(if (r is ResultadoApi.Error) EventoFormulario.Fallo(r.mensaje) else EventoFormulario.Listo)
        }
    }

    companion object {
        fun fabrica(contexto: Context, salaId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer { SalaFormViewModel(ServiceLocator.obtener(contexto).salas, salaId) }
        }
    }
}

/** Panel del coordinador: ocupación semanal por sala e inconsistencias a resolver. */
class PanelViewModel(
    private val reservas: ReservaRepository,
    hoy: () -> LocalDate = Reloj::hoy
) : ViewModel() {

    val desde: LocalDate = hoy()
    val hasta: LocalDate = desde.plusDays(6)

    private val _ocupacion = MutableStateFlow<ResultadoApi<List<Ocupacion>>>(ResultadoApi.Cargando)
    val ocupacion: StateFlow<ResultadoApi<List<Ocupacion>>> = _ocupacion.asStateFlow()

    private val _conflictos = MutableStateFlow<ResultadoApi<List<Conflicto>>>(ResultadoApi.Cargando)
    val conflictos: StateFlow<ResultadoApi<List<Conflicto>>> = _conflictos.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    fun refrescar() {
        viewModelScope.launch {
            _ocupacion.value = reservas.ocupacion(desde, hasta).mapear { lista -> lista.sortedByDescending { it.porcentaje } }
            _conflictos.value = reservas.conflictos()
        }
    }

    /** Resolver una inconsistencia = cancelar una de las reservas involucradas. */
    fun cancelar(reserva: Reserva) {
        viewModelScope.launch {
            when (val r = reservas.cancelar(reserva)) {
                is ResultadoApi.Exito -> _mensajes.emit("Se canceló \"${reserva.actividad}\" (${reserva.relator})")
                is ResultadoApi.Error -> _mensajes.emit(r.mensaje)
                ResultadoApi.Cargando -> Unit
            }
            refrescar()
        }
    }

    companion object {
        fun fabrica(contexto: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { PanelViewModel(ServiceLocator.obtener(contexto).reservas) }
        }
    }
}
