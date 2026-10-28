package cl.therionlabs.reservas.notificaciones

import cl.therionlabs.reservas.model.Reloj
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cl.therionlabs.reservas.R
import cl.therionlabs.reservas.model.Reserva
import java.time.Duration
import java.time.LocalDateTime

/** Programa/cancela el aviso local previo a una reserva. Interfaz para usar un fake en tests. */
interface ProgramadorRecordatorios {
    fun programar(reserva: Reserva)
    fun cancelar(reservaId: Long)
}

/**
 * WorkManager sobrevive a cierres de la app y reinicios del teléfono, a diferencia de un
 * simple Handler. Cada reserva tiene un trabajo único: reprogramar reemplaza el anterior.
 */
class RecordatoriosWorkManager(
    contexto: Context,
    private val minutosAntes: Long = MINUTOS_ANTES,
    private val ahora: () -> LocalDateTime = Reloj::ahora
) : ProgramadorRecordatorios {

    private val workManager = WorkManager.getInstance(contexto.applicationContext)

    override fun programar(reserva: Reserva) {
        val inicio = LocalDateTime.of(reserva.fecha, reserva.horaInicio)
        val actual = ahora()
        if (!reserva.activa || !inicio.isAfter(actual)) {
            cancelar(reserva.id)
            return
        }
        // Si faltan menos de X minutos, se avisa de inmediato.
        val espera = Duration.between(actual, inicio.minusMinutes(minutosAntes)).coerceAtLeast(Duration.ZERO)
        val trabajo = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(espera)
            .setInputData(
                workDataOf(
                    RecordatorioWorker.ID to reserva.id,
                    RecordatorioWorker.TITULO to reserva.actividad,
                    RecordatorioWorker.TEXTO to "Comienza a las ${reserva.horaInicio} en ${reserva.salaCodigo} · ${reserva.salaNombre}"
                )
            )
            .addTag(ETIQUETA)
            .build()
        workManager.enqueueUniqueWork(nombre(reserva.id), ExistingWorkPolicy.REPLACE, trabajo)
    }

    override fun cancelar(reservaId: Long) {
        workManager.cancelUniqueWork(nombre(reservaId))
    }

    private fun nombre(id: Long) = "recordatorio-$id"

    companion object {
        const val MINUTOS_ANTES = 15L
        private const val ETIQUETA = "recordatorios"
    }
}

class RecordatorioWorker(contexto: Context, params: WorkerParameters) : CoroutineWorker(contexto, params) {

    override suspend fun doWork(): Result {
        val permiso = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
        if (permiso != PackageManager.PERMISSION_GRANTED) return Result.success()

        val id = inputData.getLong(ID, 0L)
        val notificacion = NotificationCompat.Builder(applicationContext, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Tu sesión empieza pronto: ${inputData.getString(TITULO).orEmpty()}")
            .setContentText(inputData.getString(TEXTO).orEmpty())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id.toInt(), notificacion)
        return Result.success()
    }

    companion object {
        const val CANAL = "recordatorios_reservas"
        const val ID = "id"
        const val TITULO = "titulo"
        const val TEXTO = "texto"

        fun crearCanal(contexto: Context) {
            val canal = NotificationChannel(
                CANAL,
                contexto.getString(R.string.canal_recordatorios),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = contexto.getString(R.string.canal_recordatorios_desc) }
            contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }
}
