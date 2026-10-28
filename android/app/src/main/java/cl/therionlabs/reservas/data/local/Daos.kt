package cl.therionlabs.reservas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Los métodos que devuelven Flow re-emiten con cada cambio de la tabla: la UI observa Room
 * y el repositorio solo tiene que escribir lo que llega del servidor.
 */
@Dao
interface SalaDao {

    @Query("SELECT * FROM salas ORDER BY codigo")
    fun observarTodas(): Flow<List<SalaEntity>>

    @Query("SELECT * FROM salas WHERE id = :id")
    suspend fun obtener(id: Long): SalaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(salas: List<SalaEntity>)

    @Query("DELETE FROM salas")
    suspend fun borrarTodas()

    /** El servidor es la fuente de verdad: el catálogo local se reemplaza completo. */
    @Transaction
    suspend fun reemplazarTodas(salas: List<SalaEntity>) {
        borrarTodas()
        guardar(salas)
    }
}

@Dao
interface ReservaDao {

    @Query("SELECT * FROM reservas WHERE fecha = :fecha ORDER BY horaInicio")
    fun observarDelDia(fecha: String): Flow<List<ReservaEntity>>

    @Query("SELECT * FROM reservas WHERE usuarioId = :usuarioId ORDER BY fecha, horaInicio")
    fun observarDeUsuario(usuarioId: Long): Flow<List<ReservaEntity>>

    @Query("SELECT * FROM reservas WHERE salaId = :salaId AND fecha = :fecha AND estado = 'ACTIVA'")
    suspend fun activasDeSala(salaId: Long, fecha: String): List<ReservaEntity>

    @Query("SELECT * FROM reservas WHERE id = :id")
    suspend fun obtener(id: Long): ReservaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(reservas: List<ReservaEntity>)

    @Query("DELETE FROM reservas WHERE fecha = :fecha")
    suspend fun borrarDelDia(fecha: String)

    @Query("DELETE FROM reservas WHERE usuarioId = :usuarioId AND fecha >= :desde")
    suspend fun borrarDeUsuarioDesde(usuarioId: Long, desde: String)

    @Transaction
    suspend fun reemplazarDelDia(fecha: String, reservas: List<ReservaEntity>) {
        borrarDelDia(fecha)
        guardar(reservas)
    }

    @Transaction
    suspend fun reemplazarDeUsuario(usuarioId: Long, desde: String, reservas: List<ReservaEntity>) {
        borrarDeUsuarioDesde(usuarioId, desde)
        guardar(reservas)
    }
}
