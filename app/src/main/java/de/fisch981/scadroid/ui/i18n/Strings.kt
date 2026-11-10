package de.fisch981.scadroid.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

enum class AppLanguage(val id: String, val titleDe: String, val titleEn: String) {
    SYSTEM("system", "System-Standard", "System Default"),
    GERMAN("de", "Deutsch", "German"),
    ENGLISH("en", "Englisch", "English")
}

data class Strings(
    val appTitle: String,
    val render: String,
    val rendering: String,
    val menu: String,
    val newModel: String,
    val openFile: String,
    val saveCodeAs: String,
    val exportStl: String,
    val restoreSample: String,
    val clearConsole: String,
    val language: String,
    val languageSelectTitle: String,
    val version: String,
    val build: String,
    
    // Tabs
    val tabCode: String,
    val tab3dView: String,
    val tabParameters: String,
    val tabConsole: String,

    // Dialogs
    val newModelDialogTitle: String,
    val newModelDialogMessage: String,
    val newModelDialogConfirm: String,
    val cancel: String,

    val resetSampleDialogTitle: String,
    val resetSampleDialogMessage: String,
    val resetSampleDialogConfirm: String,

    // Viewport HUD
    val viewportEmpty: String,
    val viewportModelInfo: (triangles: Int, vertices: Int) -> String,
    val viewportDimensions: (x: Int, y: Int, z: Int) -> String,
    val viewportEvalTime: (timeMs: Long) -> String,
    val viewportError: (msg: String) -> String,
    val viewportIdle: String,
    val viewportCenterButton: String,

    // Customizer
    val customizerNoParams: String,
    val customizerHint: String,

    // Terminal
    val terminalHeader: String
)

val GermanStrings = Strings(
    appTitle = "SCADroid",
    render = "Render",
    rendering = "Berechne...",
    menu = "Menü",
    newModel = "Neues Modell",
    openFile = "Datei öffnen...",
    saveCodeAs = "Code speichern unter...",
    exportStl = "Als STL exportieren...",
    restoreSample = "Beispiel wiederherstellen",
    clearConsole = "Konsole leeren",
    language = "Sprache / Language",
    languageSelectTitle = "Sprache wählen / Select Language",
    version = "Version",
    build = "Build",

    tabCode = "Code",
    tab3dView = "3D-Ansicht",
    tabParameters = "Parameter",
    tabConsole = "Konsole",

    newModelDialogTitle = "Neues Modell erstellen?",
    newModelDialogMessage = "Möchtest du wirklich ein neues Modell erstellen? Der aktuelle Editor-Inhalt wird dabei gelöscht.",
    newModelDialogConfirm = "Inhalt löschen & Neu",
    cancel = "Abbrechen",

    resetSampleDialogTitle = "Beispiel laden?",
    resetSampleDialogMessage = "Möchtest du das OpenSCAD-Beispielmodell wiederherstellen? Nicht gespeicherte Änderungen gehen verloren.",
    resetSampleDialogConfirm = "Laden",

    viewportEmpty = "Modell: Keine Geometrie (Leer)",
    viewportModelInfo = { tri, vert -> "Modell: $tri Dreiecke | $vert Vertices" },
    viewportDimensions = { x, y, z -> "Abmessungen: $x × $y × $z mm" },
    viewportEvalTime = { ms -> "Auswertung: $ms ms" },
    viewportError = { msg -> "Fehler: $msg" },
    viewportIdle = "Tippe oben auf Vorschau oder Render",
    viewportCenterButton = "Modell zentrieren",

    customizerNoParams = "Keine Customizer-Parameter gefunden",
    customizerHint = "Definiere Variablen im SCAD-Code wie:\n\n/* [Geometrie] */\nbreite = 30; // [10:1:100]\nhoehe = 15; // [5:1:50]\nmit_bohrung = true;",

    terminalHeader = "OPENSCAD TERMINAL & COMPILER OUTPUT"
)

val EnglishStrings = Strings(
    appTitle = "SCADroid",
    render = "Render",
    rendering = "Rendering...",
    menu = "Menu",
    newModel = "New Model",
    openFile = "Open File...",
    saveCodeAs = "Save Code As...",
    exportStl = "Export as STL...",
    restoreSample = "Restore Sample",
    clearConsole = "Clear Console",
    language = "Language / Sprache",
    languageSelectTitle = "Select Language / Sprache wählen",
    version = "Version",
    build = "Build",

    tabCode = "Code",
    tab3dView = "3D View",
    tabParameters = "Parameters",
    tabConsole = "Console",

    newModelDialogTitle = "Create new model?",
    newModelDialogMessage = "Do you really want to create a new model? The current editor code will be erased.",
    newModelDialogConfirm = "Erase & New",
    cancel = "Cancel",

    resetSampleDialogTitle = "Load sample model?",
    resetSampleDialogMessage = "Do you want to restore the default OpenSCAD sample? Unsaved changes will be lost.",
    resetSampleDialogConfirm = "Load",

    viewportEmpty = "Model: No geometry (Empty)",
    viewportModelInfo = { tri, vert -> "Model: $tri Triangles | $vert Vertices" },
    viewportDimensions = { x, y, z -> "Dimensions: $x × $y × $z mm" },
    viewportEvalTime = { ms -> "Evaluation: $ms ms" },
    viewportError = { msg -> "Error: $msg" },
    viewportIdle = "Tap Render above to evaluate",
    viewportCenterButton = "Center Model",

    customizerNoParams = "No customizer parameters found",
    customizerHint = "Define variables in SCAD code like:\n\n/* [Geometry] */\nwidth = 30; // [10:1:100]\nheight = 15; // [5:1:50]\nwith_hole = true;",

    terminalHeader = "OPENSCAD TERMINAL & COMPILER OUTPUT"
)

val LocalStrings = compositionLocalOf { EnglishStrings }

object LanguageManager {
    private const val PREFS_NAME = "scadroid_prefs"
    private const val KEY_LANG = "pref_app_language"

    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_LANG, AppLanguage.SYSTEM.id) ?: AppLanguage.SYSTEM.id
        return AppLanguage.entries.firstOrNull { it.id == savedId } ?: AppLanguage.SYSTEM
    }

    fun setSavedLanguage(context: Context, lang: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang.id).apply()
    }

    fun resolveStrings(selectedLang: AppLanguage): Strings {
        return when (selectedLang) {
            AppLanguage.GERMAN -> GermanStrings
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.SYSTEM -> {
                val sysLang = Locale.getDefault().language.lowercase()
                if (sysLang.startsWith("de")) GermanStrings else EnglishStrings
            }
        }
    }
}
