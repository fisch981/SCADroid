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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
    var showNewProjectConfirmDialog by remember { mutableStateOf(false) }
    var showResetSampleConfirmDialog by remember { mutableStateOf(false) }

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

    val exportStlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("model/stl")
    ) { uri ->
        uri?.let { viewModel.exportStlToUri(it, context.contentResolver) }
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
                    Text(
                        text = "SCADroid",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                actions = {
                    // Render-Button: Führt CSG-Berechnung aus und wechselt direkt zur 3D-Ansicht
                    Button(
                        onClick = {
                            viewModel.render()
                            selectedTab = 1
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScadGold,
                            contentColor = Color(0xFF121212)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Render",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Render",
                            color = Color(0xFF121212),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // More Menu (Speichern, STL-Export, Öffnen, etc.)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menü",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
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
                                    showNewProjectConfirmDialog = true
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
                                text = { Text("Code speichern unter...", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    saveFileLauncher.launch(currentFileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Als STL exportieren...", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    val baseName = if (currentFileName.endsWith(".scad", ignoreCase = true)) {
                                        currentFileName.substring(0, currentFileName.length - 5)
                                    } else currentFileName
                                    exportStlLauncher.launch("$baseName.stl")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Beispiel wiederherstellen", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    showResetSampleConfirmDialog = true
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
                            androidx.compose.material3.Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Version ${de.fisch981.scadroid.BuildConfig.VERSION_NAME} (Build ${de.fisch981.scadroid.BuildConfig.VERSION_CODE})",
                                        color = Color(0xFF888888),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(18.dp)) },
                                onClick = { }
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
        // Bestätigungs-Dialog: Neues Modell (löscht aktuellen Editor-Inhalt)
        if (showNewProjectConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showNewProjectConfirmDialog = false },
                title = {
                    Text(
                        "Neues Modell erstellen?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Möchtest du wirklich ein neues Modell erstellen? Der aktuelle Editor-Inhalt wird dabei gelöscht.",
                        color = Color(0xFFDDDDDD)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNewProjectConfirmDialog = false
                            viewModel.newProject()
                            selectedTab = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Inhalt löschen & Neu", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showNewProjectConfirmDialog = false }
                    ) {
                        Text("Abbrechen", color = Color(0xFFB0B0B0))
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Bestätigungs-Dialog: Beispiel wiederherstellen
        if (showResetSampleConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetSampleConfirmDialog = false },
                title = {
                    Text(
                        "Beispiel laden?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Möchtest du das OpenSCAD-Beispielmodell wiederherstellen? Nicht gespeicherte Änderungen gehen verloren.",
                        color = Color(0xFFDDDDDD)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetSampleConfirmDialog = false
                            viewModel.resetToSample()
                            selectedTab = 0
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScadGold,
                            contentColor = Color(0xFF121212)
                        )
                    ) {
                        Text("Laden", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showResetSampleConfirmDialog = false }
                    ) {
                        Text("Abbrechen", color = Color(0xFFB0B0B0))
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }

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
