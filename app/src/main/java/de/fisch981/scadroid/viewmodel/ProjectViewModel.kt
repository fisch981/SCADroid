package de.fisch981.scadroid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.fisch981.scadroid.model.CustomizerParam
import de.fisch981.scadroid.model.RenderState
import de.fisch981.scadroid.nativebridge.OpenSCADEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectViewModel : ViewModel() {

    private val _code = MutableStateFlow(
        """// SCADroid Beispiel-Modell
${'$'}fn = 50;

difference() {
    cube([30, 30, 15], center = true);
    cylinder(r = 8, h = 20, center = true);
}
""".trimIndent()
    )
    val code: StateFlow<String> = _code.asStateFlow()

    private val _renderState = MutableStateFlow<RenderState>(RenderState.Idle)
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()

    private val _engineVersion = MutableStateFlow("Initialisiere Engine...")
    val engineVersion: StateFlow<String> = _engineVersion.asStateFlow()

    private val _customizerParams = MutableStateFlow<List<CustomizerParam>>(emptyList())
    val customizerParams: StateFlow<List<CustomizerParam>> = _customizerParams.asStateFlow()

    private val _logOutput = MutableStateFlow("SCADroid bereit.")
    val logOutput: StateFlow<String> = _logOutput.asStateFlow()

    init {
        loadEngineInfo()
    }

    private fun loadEngineInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val version = OpenSCADEngine.getEngineVersion()
                _engineVersion.value = version
                _logOutput.value = "Engine geladen: $version"
            } catch (e: Throwable) {
                _engineVersion.value = "Fehler beim Laden: ${e.message}"
                _logOutput.value = "Engine Fehler: ${e.message}"
            }
        }
    }

    fun updateCode(newCode: String) {
        _code.value = newCode
    }

    fun triggerPreview() {
        viewModelScope.launch {
            _renderState.value = RenderState.Evaluating(0.5f, "Erzeuge OpenCSG Schnellvorschau...")
            withContext(Dispatchers.Default) {
                // Hier wird der native JNI Aufruf zur Vorschau angebunden
                val startTime = System.currentTimeMillis()
                val duration = System.currentTimeMillis() - startTime
                _renderState.value = RenderState.Ready(
                    verticesCount = 280,
                    trianglesCount = 140,
                    evaluationTimeMs = duration,
                    isFullRender = false
                )
            }
        }
    }

    fun triggerFullRender() {
        viewModelScope.launch {
            _renderState.value = RenderState.Evaluating(0.1f, "Berechne exakte CGAL Geometrie...")
            withContext(Dispatchers.Default) {
                // Hier wird der native JNI Aufruf zur CGAL Mesh Generierung angebunden
                val startTime = System.currentTimeMillis()
                val duration = System.currentTimeMillis() - startTime
                _renderState.value = RenderState.Ready(
                    verticesCount = 1250,
                    trianglesCount = 2500,
                    evaluationTimeMs = duration,
                    isFullRender = true
                )
            }
        }
    }
}
