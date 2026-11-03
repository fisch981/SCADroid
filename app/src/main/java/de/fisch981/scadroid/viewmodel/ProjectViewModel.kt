package de.fisch981.scadroid.viewmodel

import androidx.lifecycle.ViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectViewModel : ViewModel() {

    private val parser = ScadParser()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _code = MutableStateFlow(
        """// SCADroid parametrisches Modell
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
    )
    val code: StateFlow<String> = _code.asStateFlow()

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
        // Bei Code-Änderung extrahiere sofort die neuen Parameter
        viewModelScope.launch(Dispatchers.Default) {
            val parseRes = parser.parseAndEvaluate(newCode, paramOverrides)
            _customizerParams.value = parseRes.parameters
        }
    }

    fun updateParameter(param: CustomizerParam) {
        when (param) {
            is CustomizerParam.NumberParam -> paramOverrides[param.name] = param.value
            is CustomizerParam.BooleanParam -> paramOverrides[param.name] = param.value
            is CustomizerParam.StringParam -> paramOverrides[param.name] = param.value
        }
        // Aktualisiere Parameter-Liste im UI
        _customizerParams.value = _customizerParams.value.map {
            if (it.name == param.name) param else it
        }
        // Live Neuberechnung bei Slider-Änderung
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
