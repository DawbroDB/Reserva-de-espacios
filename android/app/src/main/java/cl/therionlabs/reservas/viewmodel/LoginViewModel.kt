package cl.therionlabs.reservas.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.therionlabs.reservas.data.ServiceLocator
import cl.therionlabs.reservas.model.Usuario
import cl.therionlabs.reservas.repository.AuthRepository
import cl.therionlabs.reservas.repository.ResultadoApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    /** Evento único: login correcto → la navegación decide a qué pantalla ir. */
    private val _ingreso = MutableSharedFlow<Usuario>(extraBufferCapacity = 1)
    val ingreso: SharedFlow<Usuario> = _ingreso.asSharedFlow()

    fun cambiarEmail(valor: String) = _estado.update { it.copy(email = valor, error = null) }
    fun cambiarPassword(valor: String) = _estado.update { it.copy(password = valor, error = null) }

    fun ingresar() {
        val actual = _estado.value
        if (actual.cargando) return
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(actual.email.trim())) {
            _estado.update { it.copy(error = "Ingresa un correo válido") }
            return
        }
        if (actual.password.isBlank()) {
            _estado.update { it.copy(error = "Ingresa tu contraseña") }
            return
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.login(actual.email, actual.password)) {
                is ResultadoApi.Exito -> {
                    _estado.update { LoginUiState() }
                    _ingreso.emit(r.datos)
                }
                is ResultadoApi.Error -> _estado.update { it.copy(cargando = false, error = r.mensaje) }
                ResultadoApi.Cargando -> Unit
            }
        }
    }

    companion object {
        fun fabrica(contexto: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(ServiceLocator.obtener(contexto).auth) }
        }
    }
}
