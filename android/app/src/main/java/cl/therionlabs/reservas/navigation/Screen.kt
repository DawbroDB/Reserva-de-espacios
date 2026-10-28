package cl.therionlabs.reservas.navigation

import android.net.Uri
import cl.therionlabs.reservas.data.aTexto
import java.time.LocalDate
import java.time.LocalTime

/** Rutas tipo-seguras de la app (patrón sealed class de la Guía 10). */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Agenda : Screen("agenda")
    data object Buscar : Screen("buscar")
    data object MisReservas : Screen("mis-reservas")
    data object Catalogo : Screen("catalogo")
    data object Panel : Screen("panel")

    data object NuevaReserva : Screen("reserva?salaId={salaId}&fecha={fecha}&inicio={inicio}&fin={fin}&reservaId={reservaId}") {
        fun crear(salaId: Long, fecha: LocalDate, inicio: LocalTime, fin: LocalTime, reservaId: Long? = null): String =
            "reserva?salaId=$salaId&fecha=${fecha.aTexto()}" +
                "&inicio=${Uri.encode(inicio.aTexto())}&fin=${Uri.encode(fin.aTexto())}" +
                "&reservaId=${reservaId ?: -1}"
    }

    data object SalaForm : Screen("sala?salaId={salaId}") {
        fun crear(salaId: Long?): String = "sala?salaId=${salaId ?: -1}"
    }
}
