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
import cl.therionlabs.reservas.model.Sala
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.ResultadoApi
import cl.therionlabs.reservas.repository.SalaRepository
import cl.therionlabs.reservas.repository.TipoError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Datos con los que se abre el formulario (desde la agenda, la búsqueda o "editar"). */
data class ArgumentosReserva(
    val salaId: Long,
    val fecha: LocalDate,
    val inicio: LocalTime,
    val fin: LocalTime,
    val reservaId: Long? = null
)

data class NuevaReservaUiState(
    val sala: Sala? = null,
    val actividad: String = "",
    val fecha: LocalDate,
    val inicio: LocalTime,
    val fin: LocalTime,
    val editando: Boolean = false,
    val errorActividad: String? = null,
    val errorHorario: String? = null,
    /** Reserva que impide guardar (detectada localmente o informada por el servidor). */
    val conflicto: Reserva? = null,
    val enviando: Boolean = false
)

sealed interface EventoReserva {
    data class Guardada(val reserva: Reserva) : EventoReserva
    data class Mensaje(val texto: String) : EventoReserva
}

class NuevaReservaViewModel(
    private val salas: SalaRepository,
    private val reservas: ReservaRepository,
    private val usuarioId: Long,
    private val args: ArgumentosReserva,
    private val ahora: () -> LocalDateTime = Reloj::ahora
) : ViewModel() {

    private val _estado = MutableStateFlow(
        NuevaReservaUiState(fecha = args.fecha, inicio = args.inicio, fin = args.fin, editando = args.reservaId != null)
    )
    val estado: StateFlow<NuevaReservaUiState> = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoReserva>(extraBufferCapacity = 1)
    val eventos: SharedFlow<EventoReserva> = _eventos.asSharedFlow()

    init {
        viewModelScope.launch {
            val sala = salas.obtener(args.salaId)
            val existente = args.reservaId?.let { reservas.obtener(it) }
            _estado.update {
                it.copy(
                    sala = sala,
                    actividad = existente?.actividad ?: it.actividad,
                    fecha = existente?.fecha ?: it.fecha,
                    inicio = existente?.horaInicio ?: it.inicio,
                    fin = existente?.horaFin ?: it.fin
                )
            }
            // Deja la caché del día al día para que el pre-chequeo local sea confiable.
            reservas.sincronizarDia(_estado.value.fecha)
        }
    }

    fun cambiarActividad(valor: String) =
        _estado.update { it.copy(actividad = valor.take(120), errorActividad = null) }

    fun cambiarFecha(valor: LocalDate) {
        _estado.update { it.copy(fecha = valor, errorHorario = null, conflicto = null) }
        viewModelScope.launch { reservas.sincronizarDia(valor) }
    }

    fun cambiarInicio(valor: LocalTime) = _estado.update {
        val fin = if (valor < it.fin) it.fin else valor.plusHours(1)
        it.copy(inicio = valor, fin = fin, errorHorario = null, conflicto = null)
    }

    fun cambiarFin(valor: LocalTime) = _estado.update { it.copy(fin = valor, errorHorario = null, conflicto = null) }

    /** Valida el formulario y deja los mensajes en el estado. */
    internal fun validar(): Boolean {
        val e = _estado.value
        val errorActividad = when {
            e.actividad.isBlank() -> "Indica el curso o actividad"
            e.actividad.trim().length < 3 -> "El nombre es demasiado corto"
            else -> null
        }
        val errorHorario = validarRango(e.inicio, e.fin)
            ?: if (LocalDateTime.of(e.fecha, e.inicio) < ahora()) "No puedes reservar en el pasado" else null
        _estado.update { it.copy(errorActividad = errorActividad, errorHorario = errorHorario) }
        return errorActividad == null && errorHorario == null
    }

    fun guardar() {
        if (_estado.value.enviando || !validar()) return
        val e = _estado.value
        _estado.update { it.copy(enviando = true, conflicto = null) }
        viewModelScope.launch {
            val topeLocal = reservas.buscarTopeLocal(args.salaId, e.fecha, e.inicio, e.fin, args.reservaId)
            if (topeLocal != null) {
                _estado.update {
                    it.copy(enviando = false, conflicto = topeLocal, errorHorario = mensajeTope(topeLocal))
                }
                return@launch
            }
            val resultado = if (args.reservaId == null) {
                reservas.crear(args.salaId, usuarioId, e.fecha, e.inicio, e.fin, e.actividad)
            } else {
                reservas.actualizar(args.reservaId, args.salaId, usuarioId, e.fecha, e.inicio, e.fin, e.actividad)
            }
            when (resultado) {
                is ResultadoApi.Exito -> {
                    _estado.update { it.copy(enviando = false) }
                    _eventos.emit(EventoReserva.Guardada(resultado.datos))
                }
                is ResultadoApi.Error -> {
                    val tope = resultado.tipo == TipoError.TOPE_HORARIO
                    _estado.update {
                        it.copy(
                            enviando = false,
                            conflicto = if (tope) resultado.reservaEnConflicto else null,
                            errorHorario = if (tope) resultado.mensaje else it.errorHorario
                        )
                    }
                    if (!tope) _eventos.emit(EventoReserva.Mensaje(resultado.mensaje))
                }
                ResultadoApi.Cargando -> Unit
            }
        }
    }

    private fun mensajeTope(r: Reserva) =
        "Se cruza con \"${r.actividad}\" de ${r.horaInicio} a ${r.horaFin} (${r.relator})"

    companion object {
        fun fabrica(contexto: Context, usuarioId: Long, args: ArgumentosReserva): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val c = ServiceLocator.obtener(contexto)
                    NuevaReservaViewModel(c.salas, c.reservas, usuarioId, args)
                }
            }
    }
}
