package de.fisch981.scadroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.fisch981.scadroid.ui.customizer.CustomizerView
import de.fisch981.scadroid.ui.editor.CodeEditorView
import de.fisch981.scadroid.ui.viewport.ViewportScreen
import de.fisch981.scadroid.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SCADroidApp(
    viewModel: ProjectViewModel = viewModel()
) {
    val code by viewModel.code.collectAsState()
    val renderState by viewModel.renderState.collectAsState()
    val engineVersion by viewModel.engineVersion.collectAsState()
    val customizerParams by viewModel.customizerParams.collectAsState()
    val logOutput by viewModel.logOutput.collectAsState()

    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Editor, 1: 3D-View, 2: Customizer, 3: Konsole

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SCADroid", style = MaterialTheme.typography.titleMedium)
                        Text(
                            engineVersion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // F5: Schnellvorschau
                    FilledTonalButton(
                        onClick = { viewModel.triggerPreview() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "F5 Vorschau")
                        Spacer(Modifier.width(4.dp))
                        Text("Vorschau")
                    }
                    // F6: CGAL Render
                    FilledTonalButton(
                        onClick = { viewModel.triggerFullRender() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = "F6 Render")
                        Spacer(Modifier.width(4.dp))
                        Text("Render")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!isTabletOrLandscape) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                        label = { Text("Code") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ViewInAr, contentDescription = "3D View") },
                        label = { Text("3D-Ansicht") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "Customizer") },
                        label = { Text("Parameter") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = "Logs") },
                        label = { Text("Konsole") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTabletOrLandscape) {
                // Adaptive Split Screen: Editor links, Viewport rechts
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CodeEditorView(
                            code = code,
                            onCodeChange = { viewModel.updateCode(it) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        ViewportScreen(
                            renderState = renderState,
                            onResetCamera = {}
                        )
                    }
                }
            } else {
                // Phone Screen: Tab-Navigation
                when (selectedTab) {
                    0 -> CodeEditorView(
                        code = code,
                        onCodeChange = { viewModel.updateCode(it) }
                    )
                    1 -> ViewportScreen(
                        renderState = renderState,
                        onResetCamera = {}
                    )
                    2 -> CustomizerView(
                        parameters = customizerParams,
                        onParameterChanged = {}
                    )
                    3 -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = logOutput,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
