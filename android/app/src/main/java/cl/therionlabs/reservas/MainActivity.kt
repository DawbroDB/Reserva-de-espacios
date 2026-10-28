package cl.therionlabs.reservas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import cl.therionlabs.reservas.navigation.AppNavigation
import cl.therionlabs.reservas.ui.theme.ReservasTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReservasTheme {
                AppNavigation(anchoVentana = calculateWindowSizeClass(this).widthSizeClass)
            }
        }
    }
}
