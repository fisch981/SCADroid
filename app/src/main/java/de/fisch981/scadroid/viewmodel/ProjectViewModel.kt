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
    private val wasmEngine by lazy {
        de.fisch981.scadroid.engine.OpenScadWasmEngine(application) { msg ->
            viewModelScope.launch(Dispatchers.Main) {
                appendLog("[OpenSCAD] $msg")
            }
        }
    }
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
        evaluateInternal()
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
            _customizerParams.value = parser.extractParameters(newCode)
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
        val emptyTemplate = "\$fn = 32;\n"
        _code.value = emptyTemplate
        _currentFileName.value = "neues_modell.scad"
        prefs.edit().putString("current_file_name", "neues_modell.scad").apply()
        saveCodeInternal(emptyTemplate)
        appendLog("Neues Modell erstellt (\$fn = 32;).")
        _statusMessage.value = "Neues Modell erstellt"
        evaluateInternal()
    }

    fun resetToSample() {
        _code.value = DEFAULT_SAMPLE_CODE
        _currentFileName.value = "beispiel_modell.scad"
        prefs.edit().putString("current_file_name", "beispiel_modell.scad").apply()
        saveCodeInternal(DEFAULT_SAMPLE_CODE)
        appendLog("Beispiel-Modell geladen.")
        _statusMessage.value = "Beispiel geladen"
        evaluateInternal()
    }

    fun loadFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "modell.scad"
                if (fileName.endsWith(".stl", ignoreCase = true)) {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    val destFile = File(getApplication<Application>().filesDir, fileName)
                    destFile.writeBytes(bytes)
                    withContext(Dispatchers.Main) {
                        appendLog("STL-Datei '$fileName' (${bytes.size} Bytes) für import(\"$fileName\") im Projektverzeichnis bereitgestellt.")
                        _statusMessage.value = "'$fileName' für import() bereitgestellt"
                        evaluateInternal()
                    }
                    return@launch
                }
                val inputStream = contentResolver.openInputStream(uri)
                val loadedCode = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                withContext(Dispatchers.Main) {
                    _code.value = loadedCode
                    _currentFileName.value = fileName
                    prefs.edit().putString("current_file_name", fileName).apply()
                    saveCodeInternal(loadedCode)
                    appendLog("Datei '$fileName' erfolgreich geladen (${loadedCode.length} Zeichen).")
                    _statusMessage.value = "'$fileName' geladen"
                    evaluateInternal()
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

    fun exportStlToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mesh = _currentMesh.value
                if (mesh.triangleCount == 0) {
                    withContext(Dispatchers.Main) {
                        appendLog("STL-Export abgebrochen: Kein 3D-Modell berechnet.")
                        _statusMessage.value = "Kein 3D-Modell zum Exportieren vorhanden"
                    }
                    return@launch
                }

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    mesh.exportBinaryStl(outputStream)
                }

                val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "modell.stl"
                withContext(Dispatchers.Main) {
                    appendLog("STL erfolgreich exportiert nach '$fileName' (${mesh.triangleCount} Dreiecke).")
                    _statusMessage.value = "'$fileName' erfolgreich exportiert"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLog("Fehler beim STL-Export: ${e.message}")
                    _statusMessage.value = "Fehler beim STL-Export"
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
        evaluateInternal()
    }

    fun render() {
        evaluateInternal()
    }

    private fun evaluateInternal() {
        viewModelScope.launch {
            _renderState.value = RenderState.Evaluating(0.2f, "Berechne 3D-Modell (OpenSCAD Core)...")
            appendLog("Starte 1:1 3D-Berechnung (OpenSCAD C++ / Manifold-Kern)...")

            val startTime = System.currentTimeMillis()

            try {
                val currentCode = _code.value
                val params = withContext(Dispatchers.Default) {
                    parser.extractParameters(currentCode)
                }
                _customizerParams.value = params

                // Ersetze geänderte Customizer-Parameter im Code
                var codeToRender = currentCode
                for ((k, v) in paramOverrides) {
                    val regex = Regex("(?m)^\\s*${Regex.escape(k)}\\s*=.*?;")
                    codeToRender = codeToRender.replace(regex, "$k = $v;")
                }

                // Führe Berechnung ausschließlich mit dem echten OpenSCAD WASM-Kern aus (1:1 Desktop-Parität)
                val stlBytes = wasmEngine.renderBinaryStl(codeToRender)

                val mesh = if (stlBytes.isNotEmpty()) {
                    withContext(Dispatchers.Default) {
                        TriangleMesh.fromBinaryStl(stlBytes)
                    }
                } else {
                    TriangleMesh.EMPTY
                }

                val duration = System.currentTimeMillis() - startTime
                _currentMesh.value = mesh
                _renderState.value = RenderState.Ready(
                    verticesCount = mesh.vertexCount,
                    trianglesCount = mesh.triangleCount,
                    evaluationTimeMs = duration,
                    isFullRender = true
                )

                appendLog("Modell fertiggestellt (Offizieller OpenSCAD Kern): ${mesh.triangleCount} Dreiecke (${mesh.vertexCount} Vertices) in ${duration} ms.")
            } catch (e: Throwable) {
                _renderState.value = RenderState.Error("Fehler bei OpenSCAD-Berechnung: ${e.message}")
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
