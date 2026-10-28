package cl.therionlabs.reservas.data

import android.content.Context
import cl.therionlabs.reservas.model.Rol
import cl.therionlabs.reservas.model.Usuario

/** Guarda quién inició sesión. Interfaz para poder usar un fake en los tests. */
interface AlmacenSesion {
    fun leer(): Usuario?
    fun guardar(usuario: Usuario)
    fun limpiar()
}

/** Implementación con SharedPreferences: solo id, nombre, correo y rol (nunca la contraseña). */
class PreferenciasSesion(contexto: Context) : AlmacenSesion {

    private val prefs = contexto.applicationContext.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    override fun leer(): Usuario? {
        val id = prefs.getLong("id", -1L)
        if (id < 0) return null
        return Usuario(
            id = id,
            email = prefs.getString("email", "").orEmpty(),
            nombre = prefs.getString("nombre", "").orEmpty(),
            rol = runCatching { Rol.valueOf(prefs.getString("rol", "").orEmpty()) }.getOrDefault(Rol.RELATOR)
        )
    }

    override fun guardar(usuario: Usuario) {
        prefs.edit()
            .putLong("id", usuario.id)
            .putString("email", usuario.email)
            .putString("nombre", usuario.nombre)
            .putString("rol", usuario.rol.name)
            .apply()
    }

    override fun limpiar() {
        prefs.edit().clear().apply()
    }
}
