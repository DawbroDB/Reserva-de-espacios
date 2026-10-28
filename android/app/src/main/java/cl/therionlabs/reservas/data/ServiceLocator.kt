package cl.therionlabs.reservas.data

import android.content.Context
import cl.therionlabs.reservas.data.local.AppDatabase
import cl.therionlabs.reservas.data.remote.ApiClient
import cl.therionlabs.reservas.notificaciones.RecordatoriosWorkManager
import cl.therionlabs.reservas.repository.AuthRepository
import cl.therionlabs.reservas.repository.ReservaRepository
import cl.therionlabs.reservas.repository.SalaRepository

/**
 * Contenedor mínimo de dependencias (el proyecto no usa Hilt). Crea una sola instancia de
 * cada repositorio para toda la app, así todos comparten la misma base de datos.
 */
object ServiceLocator {

    @Volatile
    private var contenedor: Contenedor? = null

    class Contenedor(contexto: Context) {
        private val db = AppDatabase.obtener(contexto)
        val auth = AuthRepository(ApiClient.api, PreferenciasSesion(contexto))
        val salas = SalaRepository(ApiClient.api, db.salaDao())
        val reservas = ReservaRepository(ApiClient.api, db.reservaDao(), RecordatoriosWorkManager(contexto))
    }

    fun obtener(contexto: Context): Contenedor =
        contenedor ?: synchronized(this) {
            contenedor ?: Contenedor(contexto.applicationContext).also { contenedor = it }
        }
}
