package de.fisch981.scadroid

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import de.fisch981.scadroid.ui.SCADroidApp
import de.fisch981.scadroid.ui.theme.SCADroidTheme
import de.fisch981.scadroid.viewmodel.ProjectViewModel

class MainActivity : ComponentActivity() {

    private val projectViewModel: ProjectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }
        setContent {
            SCADroidTheme {
                SCADroidApp(viewModel = projectViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_EDIT && action != Intent.ACTION_SEND) {
            return
        }

        val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        val clipUri: Uri? = intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        val targetUri: Uri? = intent.data ?: streamUri ?: clipUri

        if (targetUri != null) {
            projectViewModel.loadFromUri(targetUri, contentResolver)
        } else if (action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                projectViewModel.updateCode(sharedText)
                projectViewModel.render()
            }
        }
    }
}
