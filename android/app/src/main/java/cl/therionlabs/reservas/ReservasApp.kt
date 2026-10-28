package cl.therionlabs.reservas

import android.app.Application
import cl.therionlabs.reservas.notificaciones.RecordatorioWorker

class ReservasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // El canal debe existir antes de publicar la primera notificación (Android 8+).
        RecordatorioWorker.crearCanal(this)
    }
}
