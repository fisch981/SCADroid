package de.fisch981.scadroid.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.fisch981.scadroid.ui.customizer.CustomizerView
import de.fisch981.scadroid.ui.editor.CodeEditorView
import de.fisch981.scadroid.ui.i18n.AppLanguage
import de.fisch981.scadroid.ui.i18n.LanguageManager
import de.fisch981.scadroid.ui.i18n.LocalStrings
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(LanguageManager.getSavedLanguage(context)) }

    val strings = LanguageManager.resolveStrings(currentLanguage)

    val (versionName, versionCode) = remember(context) {
        try {
            val pm = context.packageManager
            val pkgInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            val name = pkgInfo.versionName ?: de.fisch981.scadroid.BuildConfig.VERSION_NAME
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            Pair(name, code)
        } catch (_: Exception) {
            Pair(de.fisch981.scadroid.BuildConfig.VERSION_NAME, de.fisch981.scadroid.BuildConfig.VERSION_CODE.toLong())
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
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
                    val isEvaluating = renderState is de.fisch981.scadroid.model.RenderState.Evaluating
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
                        if (isEvaluating) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF121212)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                strings.rendering,
                                color = Color(0xFF121212),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = strings.render,
                                tint = Color(0xFF121212),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                strings.render,
                                color = Color(0xFF121212),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Schnell-Speichern Button (Direktes Speichern bzw. Einmalige Ortsabfrage)
                    IconButton(
                        onClick = {
                            viewModel.saveFile(context.contentResolver) {
                                saveFileLauncher.launch(currentFileName)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = strings.saveCode,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Menu (Speichern, STL-Export, Öffnen, Sprache etc.)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = strings.menu,
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
                                text = { Text(strings.newModel, color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    showNewProjectConfirmDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.openFile, color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    openFileLauncher.launch(arrayOf("*/*", "application/x-openscad", "text/plain"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.saveCode, color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.saveFile(context.contentResolver) {
                                        saveFileLauncher.launch(currentFileName)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.saveCodeAs, color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    saveFileLauncher.launch(currentFileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.exportStl, color = Color.White) },
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
                                text = { Text(strings.restoreSample, color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    showResetSampleConfirmDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.clearConsole, color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFAAAAAA)) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.clearLogs()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.language, color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = ScadGold) },
                                onClick = {
                                    menuExpanded = false
                                    showLanguageDialog = true
                                }
                            )
                            HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${strings.version} $versionName (${strings.build} $versionCode)",
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
                    icon = { Icon(Icons.Default.Code, contentDescription = strings.tabCode) },
                    label = { Text(strings.tabCode) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ViewInAr, contentDescription = strings.tab3dView) },
                    label = { Text(strings.tab3dView) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = strings.tabParameters) },
                    label = { Text(strings.tabParameters) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = strings.tabConsole) },
                    label = { Text(strings.tabConsole) },
                    colors = navColors
                )
            }
        }
    ) { innerPadding ->
        // Bestätigungs-Dialog: Neues Modell (löscht aktuellen Editor-Inhalt)
        if (showNewProjectConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showNewProjectConfirmDialog = false },
                title = {
                    Text(
                        strings.newModelDialogTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        strings.newModelDialogMessage,
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
                        Text(strings.newModelDialogConfirm, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showNewProjectConfirmDialog = false }
                    ) {
                        Text(strings.cancel, color = Color(0xFFB0B0B0))
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
                        strings.resetSampleDialogTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        strings.resetSampleDialogMessage,
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
                        Text(strings.resetSampleDialogConfirm, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showResetSampleConfirmDialog = false }
                    ) {
                        Text(strings.cancel, color = Color(0xFFB0B0B0))
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Dialog: Sprache auswählen (System / DE / EN)
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = {
                    Text(
                        strings.languageSelectTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = currentLanguage == lang
                            val label = when (lang) {
                                AppLanguage.SYSTEM -> "${lang.titleDe} / ${lang.titleEn}"
                                AppLanguage.GERMAN -> "${lang.titleDe} (${lang.titleEn})"
                                AppLanguage.ENGLISH -> "${lang.titleEn} (${lang.titleDe})"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) ScadGold.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        currentLanguage = lang
                                        LanguageManager.setSavedLanguage(context, lang)
                                        showLanguageDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        currentLanguage = lang
                                        LanguageManager.setSavedLanguage(context, lang)
                                        showLanguageDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ScadGold,
                                        unselectedColor = Color(0xFF888888)
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) ScadGold else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(strings.cancel, color = Color(0xFFB0B0B0))
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
                                    text = strings.terminalHeader,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearLogs() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = strings.clearConsole, tint = Color(0xFFAAAAAA))
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
    } // CompositionLocalProvider
}
