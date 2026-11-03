package de.fisch981.scadroid.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.fisch981.scadroid.engine.ScadParser
import de.fisch981.scadroid.engine.TriangleMesh
import de.fisch981.scadroid.model.CustomizerParam
import de.fisch981.scadroid.model.RenderState
import de.fisch981.scadroid.nativebridge.OpenSCADEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val parser = ScadParser()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val prefs = application.getSharedPreferences("scadroid_prefs", android.content.Context.MODE_PRIVATE)
    private val projectFile = File(application.filesDir, "current_project.scad")

    companion object {
        val DEFAULT_SAMPLE_CODE = """
            // SCADroid parametrisches Modell
            /* [Geometrie] */
            breite = 30; // [10:1:60]
            hoehe = 15; // [5:1:40]
            bohrung = 7; // [2:1:14]

            /* [Qualitaet] */
            ${'$'}fn = 32;

            difference() {
                cube([breite, breite, hoehe], center = true);
                cylinder(r = bohrung, h = hoehe + 4, center = true);
            }
        """.trimIndent()
    }

    private val _code = MutableStateFlow(loadInitialCode())
    val code: StateFlow<String> = _code.asStateFlow()

    private val _currentFileName = MutableStateFlow<String>(prefs.getString("current_file_name", "modell.scad") ?: "modell.scad")
    val currentFileName: StateFlow<String> = _currentFileName.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _renderState = MutableStateFlow<RenderState>(RenderState.Idle)
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()

    private val _currentMesh = MutableStateFlow(TriangleMesh.EMPTY)
    val currentMesh: StateFlow<TriangleMesh> = _currentMesh.asStateFlow()

    private val _engineVersion = MutableStateFlow("SCADroid Native Engine (CGAL & OpenCSG)")
    val engineVersion: StateFlow<String> = _engineVersion.asStateFlow()

    private val _customizerParams = MutableStateFlow<List<CustomizerParam>>(emptyList())
    val customizerParams: StateFlow<List<CustomizerParam>> = _customizerParams.asStateFlow()

    private val paramOverrides = mutableMapOf<String, Any>()

    private val _logOutput = MutableStateFlow("[${timeFormat.format(Date())}] SCADroid IDE initialisiert.\n[${timeFormat.format(Date())}] Berechne 3D-Initialmodell...")
    val logOutput: StateFlow<String> = _logOutput.asStateFlow()

    init {
        loadEngineInfo()
        evaluateInternal(isFull = false)
    }

    private fun loadInitialCode(): String {
        val saved = prefs.getString("saved_code", null)
        if (!saved.isNullOrBlank()) {
            return saved
        }
        return try {
            if (projectFile.exists() && projectFile.length() > 0) {
                projectFile.readText()
            } else {
                DEFAULT_SAMPLE_CODE
            }
        } catch (_: Exception) {
            DEFAULT_SAMPLE_CODE
        }
    }

    private fun loadEngineInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val version = OpenSCADEngine.getEngineVersion()
                _engineVersion.value = version
                appendLog("Native NDK-Bibliothek geladen: $version")
            } catch (e: Throwable) {
                _engineVersion.value = "OpenSCAD Engine v0.1.0"
                appendLog("NDK-Hinweis: ${e.message}")
            }
        }
    }

    fun updateCode(newCode: String) {
        _code.value = newCode
        saveCodeInternal(newCode)
        viewModelScope.launch(Dispatchers.Default) {
            val parseRes = parser.parseAndEvaluate(newCode, paramOverrides)
            _customizerParams.value = parseRes.parameters
        }
    }

    private fun saveCodeInternal(codeToSave: String) {
        prefs.edit().putString("saved_code", codeToSave).apply()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                projectFile.writeText(codeToSave)
            } catch (e: Exception) {
                appendLog("Auto-Save Fehler: ${e.message}")
            }
        }
    }

    fun saveCurrentProject() {
        saveCodeInternal(_code.value)
        appendLog("Projekt erfolgreich gespeichert.")
        _statusMessage.value = "Projekt gespeichert"
    }

    fun newProject() {
        val emptyTemplate = """
            // Neues SCADroid Modell
            ${'$'}fn = 32;

            cube([20, 20, 10], center = true);
        """.trimIndent()
        _code.value = emptyTemplate
        _currentFileName.value = "neues_modell.scad"
        prefs.edit().putString("current_file_name", "neues_modell.scad").apply()
        saveCodeInternal(emptyTemplate)
        appendLog("Neues Projekt erstellt.")
        _statusMessage.value = "Neues Projekt erstellt"
        evaluateInternal(isFull = false)
    }

    fun resetToSample() {
        _code.value = DEFAULT_SAMPLE_CODE
        _currentFileName.value = "beispiel_modell.scad"
        prefs.edit().putString("current_file_name", "beispiel_modell.scad").apply()
        saveCodeInternal(DEFAULT_SAMPLE_CODE)
        appendLog("Beispiel-Modell geladen.")
        _statusMessage.value = "Beispiel geladen"
        evaluateInternal(isFull = false)
    }

    fun loadFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val loadedCode = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "modell.scad"
                withContext(Dispatchers.Main) {
                    _code.value = loadedCode
                    _currentFileName.value = fileName
                    prefs.edit().putString("current_file_name", fileName).apply()
                    saveCodeInternal(loadedCode)
                    appendLog("Datei '$fileName' erfolgreich geladen (${loadedCode.length} Zeichen).")
                    _statusMessage.value = "'$fileName' geladen"
                    evaluateInternal(isFull = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLog("Fehler beim Öffnen der Datei: ${e.message}")
                    _statusMessage.value = "Fehler beim Laden"
                }
            }
        }
    }

    fun saveToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(_code.value.toByteArray(Charsets.UTF_8))
                }
                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "modell.scad"
                withContext(Dispatchers.Main) {
                    _currentFileName.value = fileName
                    prefs.edit().putString("current_file_name", fileName).apply()
                    saveCodeInternal(_code.value)
                    appendLog("Datei erfolgreich gespeichert nach '$fileName'.")
                    _statusMessage.value = "'$fileName' gespeichert"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLog("Fehler beim Speichern der Datei: ${e.message}")
                    _statusMessage.value = "Fehler beim Speichern"
                }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateParameter(param: CustomizerParam) {
        when (param) {
            is CustomizerParam.NumberParam -> paramOverrides[param.name] = param.value
            is CustomizerParam.BooleanParam -> paramOverrides[param.name] = param.value
            is CustomizerParam.StringParam -> paramOverrides[param.name] = param.value
        }
        _customizerParams.value = _customizerParams.value.map {
            if (it.name == param.name) param else it
        }
        evaluateInternal(isFull = false)
    }

    fun triggerPreview() {
        evaluateInternal(isFull = false)
    }

    fun triggerFullRender() {
        evaluateInternal(isFull = true)
    }

    private fun evaluateInternal(isFull: Boolean) {
        viewModelScope.launch {
            val modeName = if (isFull) "CGAL Render (F6)" else "OpenCSG Vorschau (F5)"
            _renderState.value = RenderState.Evaluating(0.2f, "Berechne $modeName...")
            appendLog("Starte $modeName...")

            val startTime = System.currentTimeMillis()

            try {
                val currentCode = _code.value
                val result = withContext(Dispatchers.Default) {
                    val parseResult = parser.parseAndEvaluate(currentCode, paramOverrides)
                    val mesh = TriangleMesh.fromSolid(parseResult.solid)
                    Pair(parseResult, mesh)
                }

                val parseResult = result.first
                val mesh = result.second
                val duration = System.currentTimeMillis() - startTime

                _customizerParams.value = parseResult.parameters
                _currentMesh.value = mesh
                _renderState.value = RenderState.Ready(
                    verticesCount = mesh.vertexCount,
                    trianglesCount = mesh.triangleCount,
                    evaluationTimeMs = duration,
                    isFullRender = isFull
                )

                appendLog("Erfolgreich abgeschlossen: ${mesh.triangleCount} Dreiecke in ${duration} ms.")
            } catch (e: Throwable) {
                _renderState.value = RenderState.Error("Fehler bei Geometrieberechnung: ${e.message}")
                appendLog("FEHLER: ${e.message}")
            }
        }
    }

    private fun appendLog(msg: String) {
        val timestamp = timeFormat.format(Date())
        _logOutput.value = "${_logOutput.value}\n[$timestamp] $msg"
    }

    fun clearLogs() {
        _logOutput.value = "[${timeFormat.format(Date())}] Konsole geleert."
    }
}
