package de.fisch981.scadroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.fisch981.scadroid.ui.SCADroidApp
import de.fisch981.scadroid.ui.theme.SCADroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SCADroidTheme {
                SCADroidApp()
            }
        }
    }
}
