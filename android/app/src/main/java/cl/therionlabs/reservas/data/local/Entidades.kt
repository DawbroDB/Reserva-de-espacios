package cl.therionlabs.reservas.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Caché local (Room/SQLite) del catálogo y la agenda: permite consultar sin conexión.
 * Fecha "yyyy-MM-dd" y horas "HH:mm" como texto: ordenan bien y se comparan en SQL.
 */
@Entity(tableName = "salas")
data class SalaEntity(
    @PrimaryKey val id: Long,
    val codigo: String,
    val nombre: String,
    val tipo: String,
    val capacidad: Int,
    val proyector: Boolean,
    val computadores: Boolean,
    val climatizacion: Boolean,
    val activa: Boolean
)

@Entity(
    tableName = "reservas",
    indices = [Index("fecha"), Index("usuarioId")]
)
data class ReservaEntity(
    @PrimaryKey val id: Long,
    val salaId: Long,
    val salaCodigo: String,
    val salaNombre: String,
    val usuarioId: Long,
    val relator: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val actividad: String,
    val estado: String
)
