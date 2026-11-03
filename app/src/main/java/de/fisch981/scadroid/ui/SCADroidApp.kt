package de.fisch981.scadroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.fisch981.scadroid.ui.customizer.CustomizerView
import de.fisch981.scadroid.ui.editor.CodeEditorView
import de.fisch981.scadroid.ui.theme.ConsoleText
import de.fisch981.scadroid.ui.theme.DarkBackground
import de.fisch981.scadroid.ui.theme.DarkBorder
import de.fisch981.scadroid.ui.theme.DarkSurface
import de.fisch981.scadroid.ui.theme.ScadGold
import de.fisch981.scadroid.ui.viewport.ViewportScreen
import de.fisch981.scadroid.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SCADroidApp(
    viewModel: ProjectViewModel = viewModel()
) {
    val code by viewModel.code.collectAsState()
    val renderState by viewModel.renderState.collectAsState()
    val currentMesh by viewModel.currentMesh.collectAsState()
    val engineVersion by viewModel.engineVersion.collectAsState()
    val customizerParams by viewModel.customizerParams.collectAsState()
    val logOutput by viewModel.logOutput.collectAsState()

    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Editor, 1: 3D-View, 2: Customizer, 3: Konsole

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SCADroid",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = engineVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = ScadGold
                        )
                    }
                },
                actions = {
                    // F5: Schnellvorschau
                    OutlinedButton(
                        onClick = { viewModel.triggerPreview() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF22252C),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E434F)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "F5 Vorschau",
                            tint = ScadGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Vorschau", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    // F6: CGAL Render
                    Button(
                        onClick = { viewModel.triggerFullRender() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScadGold,
                            contentColor = Color(0xFF121212)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ViewInAr,
                            contentDescription = "F6 Render",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Render", color = Color(0xFF121212), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF16181D)
                )
            )
        },
        bottomBar = {
            if (!isTabletOrLandscape) {
                NavigationBar(
                    containerColor = Color(0xFF16181D),
                    contentColor = Color.White
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF121212),
                        selectedTextColor = ScadGold,
                        indicatorColor = ScadGold,
                        unselectedIconColor = Color(0xFFAAAAAA),
                        unselectedTextColor = Color(0xFFAAAAAA)
                    )

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                        label = { Text("Code") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ViewInAr, contentDescription = "3D View") },
                        label = { Text("3D-Ansicht") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "Customizer") },
                        label = { Text("Parameter") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = "Logs") },
                        label = { Text("Konsole") },
                        colors = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
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
                            mesh = currentMesh
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
                        mesh = currentMesh
                    )
                    2 -> CustomizerView(
                        parameters = customizerParams,
                        onParameterChanged = { viewModel.updateParameter(it) }
                    )
                    3 -> {
                        // Professionelles Dark Terminal für Logs und Ausgaben
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D0F12))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = ScadGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "OPENSCAD TERMINAL & COMPILER OUTPUT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearLogs() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Konsole leeren", tint = Color(0xFFAAAAAA))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF08090C), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .padding(12.dp)
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    text = logOutput,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = ConsoleText,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
