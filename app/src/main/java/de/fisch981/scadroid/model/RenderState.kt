package de.fisch981.scadroid.model

sealed interface RenderState {
    data object Idle : RenderState
    data class Evaluating(val progress: Float = 0f, val message: String = "Berechne Geometrie...") : RenderState
    data class Ready(
        val verticesCount: Int = 0,
        val trianglesCount: Int = 0,
        val evaluationTimeMs: Long = 0L,
        val isFullRender: Boolean = false
    ) : RenderState
    data class Error(val message: String, val line: Int = -1) : RenderState
}
