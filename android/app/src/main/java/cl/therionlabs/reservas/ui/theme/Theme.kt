package cl.therionlabs.reservas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta corporativa sobria: azul THERIONLABS + verde "libre" / ámbar "propia" en la agenda.
val AzulPrimario = Color(0xFF0D47A1)
val AzulContenedor = Color(0xFFD6E3FF)
val AzulSobreContenedor = Color(0xFF001B3E)
val VerdeLibre = Color(0xFF2E7D32)
val VerdeLibreFondo = Color(0xFFE3F2E5)
val VerdeLibreFondoOscuro = Color(0xFF1E3A22)
val AmbarPropia = Color(0xFFFFE0B2)
val AmbarPropiaOscuro = Color(0xFF5D4037)

private val ColoresClaro = lightColorScheme(
    primary = AzulPrimario,
    onPrimary = Color.White,
    primaryContainer = AzulContenedor,
    onPrimaryContainer = AzulSobreContenedor,
    secondary = VerdeLibre,
    onSecondary = Color.White,
    tertiary = Color(0xFFEF6C00),
    onTertiary = Color.White,
    error = Color(0xFFB3261E)
)

private val ColoresOscuro = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF00468C),
    onPrimaryContainer = AzulContenedor,
    secondary = Color(0xFF8FD694),
    tertiary = Color(0xFFFFB77C)
)

@Composable
fun ReservasTheme(
    temaOscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (temaOscuro) ColoresOscuro else ColoresClaro,
        typography = Typography(),
        content = contenido
    )
}

/** Colores de celda de la agenda que dependen del tema. */
object ColoresAgenda {
    @Composable
    fun libre(): Color = if (isSystemInDarkTheme()) VerdeLibreFondoOscuro else VerdeLibreFondo

    @Composable
    fun propia(): Color = if (isSystemInDarkTheme()) AmbarPropiaOscuro else AmbarPropia
}
