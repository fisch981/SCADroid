package de.fisch981.scadroid.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val code by viewModel.code.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val renderState by viewModel.renderState.collectAsState()
    val currentMesh by viewModel.currentMesh.collectAsState()
    val engineVersion by viewModel.engineVersion.collectAsState()
    val customizerParams by viewModel.customizerParams.collectAsState()
    val logOutput by viewModel.logOutput.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // SAF File Pickers: Open & Save
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadFromUri(it, context.contentResolver) }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-openscad")
    ) { uri ->
        uri?.let { viewModel.saveToUri(it, context.contentResolver) }
    }

    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Editor, 1: 3D-View, 2: Customizer, 3: Konsole

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            text = if (currentFileName != "modell.scad" && currentFileName != "current_project.scad") currentFileName else engineVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = ScadGold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Speichern-Button: Fragt nach Dateiname & Speicherort
                    IconButton(
                        onClick = { saveFileLauncher.launch(currentFileName) }
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Speichern unter...",
                            tint = ScadGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // F5: Schnellvorschau (wechselt direkt zur 3D-Ansicht)
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerPreview()
                            selectedTab = 1
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF22252C),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E434F)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "F5 Vorschau",
                            tint = ScadGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Vorschau", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    }

                    // F6: CGAL Render (wechselt direkt zur 3D-Ansicht)
                    Button(
                        onClick = {
                            viewModel.triggerFullRender()
                            selectedTab = 1
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScadGold,
                            contentColor = Color(0xFF121212)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.ViewInAr,
                            contentDescription = "F6 Render",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Render", color = Color(0xFF121212), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    // More Menu (File & Project Actions)
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menü",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Neues Modell", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.newProject()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Datei öffnen...", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    openFileLauncher.launch(arrayOf("*/*", "application/x-openscad", "text/plain"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportieren / Speichern unter...", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    saveFileLauncher.launch(currentFileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Beispiel wiederherstellen", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.resetToSample()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Konsole leeren", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFAAAAAA)) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.clearLogs()
                                }
                            )
                        }
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
