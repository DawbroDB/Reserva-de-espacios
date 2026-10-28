package cl.therionlabs.reservas.repository

import cl.therionlabs.reservas.data.aModelo
import cl.therionlabs.reservas.data.remote.dto.ErrorDto
import cl.therionlabs.reservas.model.Reserva
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Estados de una llamada al servidor, tal como los consume la UI. */
sealed class ResultadoApi<out T> {
    data object Cargando : ResultadoApi<Nothing>()
    data class Exito<T>(val datos: T) : ResultadoApi<T>()
    data class Error(
        val mensaje: String,
        val tipo: TipoError = TipoError.OTRO,
        /** En un tope (409), la reserva existente con la que se cruza. */
        val reservaEnConflicto: Reserva? = null
    ) : ResultadoApi<Nothing>()
}

/** Transforma el dato de un Exito; Error y Cargando pasan tal cual. */
inline fun <T, R> ResultadoApi<T>.mapear(transformar: (T) -> R): ResultadoApi<R> = when (this) {
    is ResultadoApi.Exito -> ResultadoApi.Exito(transformar(datos))
    is ResultadoApi.Error -> this
    ResultadoApi.Cargando -> ResultadoApi.Cargando
}

enum class TipoError { SIN_CONEXION, TOPE_HORARIO, VALIDACION, NO_AUTORIZADO, OTRO }

private val gson = Gson()

/**
 * Ejecuta una llamada y traduce cualquier falla a ResultadoApi.Error con un mensaje legible.
 * La cancelación de corrutinas se re-lanza: tragársela rompería el ciclo de vida del ViewModel.
 */
internal suspend fun <T> llamar(bloque: suspend () -> T): ResultadoApi<T> =
    try {
        ResultadoApi.Exito(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        errorHttp(e)
    } catch (e: IOException) {
        ResultadoApi.Error("Sin conexión con el servidor. Mostrando datos guardados.", TipoError.SIN_CONEXION)
    } catch (e: Exception) {
        ResultadoApi.Error(e.message ?: "Error inesperado")
    }

private fun errorHttp(e: HttpException): ResultadoApi.Error {
    val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val error = runCatching { gson.fromJson(cuerpo, ErrorDto::class.java) }.getOrNull()
    val tipo = when (e.code()) {
        409 -> TipoError.TOPE_HORARIO
        400 -> TipoError.VALIDACION
        401 -> TipoError.NO_AUTORIZADO
        else -> TipoError.OTRO
    }
    return ResultadoApi.Error(
        mensaje = error?.mensaje ?: "El servidor respondió con error ${e.code()}",
        tipo = tipo,
        reservaEnConflicto = error?.reservaEnConflicto?.aModelo()
    )
}
