package cl.therionlabs.reservas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos SQLite local. Singleton estándar de Room:
 * `@Volatile` + `synchronized` garantizan una sola instancia en todo el proceso.
 */
@Database(entities = [SalaEntity::class, ReservaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun salaDao(): SalaDao
    abstract fun reservaDao(): ReservaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(contexto: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "reservas.db"
                ).fallbackToDestructiveMigration().build().also { instancia = it }
            }
        }
    }
}
