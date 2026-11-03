package de.fisch981.scadroid.ui.viewport

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.fisch981.scadroid.model.RenderState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun ViewportScreen(
    renderState: RenderState,
    onResetCamera: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var rotationX by remember { mutableFloatStateOf(30f) }
    var rotationY by remember { mutableFloatStateOf(45f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    rotationY += pan.x * 0.5f
                    rotationX = (rotationX + pan.y * 0.5f).coerceIn(-89f, 89f)
                    zoomScale = (zoomScale * zoom).coerceIn(0.2f, 5.0f)
                }
            }
    ) {
        // Native OpenGL ES SurfaceView
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    setRenderer(object : GLSurfaceView.Renderer {
                        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                            android.opengl.GLES30.glClearColor(0.12f, 0.12f, 0.14f, 1.0f)
                            android.opengl.GLES30.glEnable(android.opengl.GLES30.GL_DEPTH_TEST)
                        }

                        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                            android.opengl.GLES30.glViewport(0, 0, width, height)
                        }

                        override fun onDrawFrame(gl: GL10?) {
                            android.opengl.GLES30.glClear(
                                android.opengl.GLES30.GL_COLOR_BUFFER_BIT or android.opengl.GLES30.GL_DEPTH_BUFFER_BIT
                            )
                        }
                    })
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay HUD & Stats
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "3D Ansicht (OpenGL ES 3.0)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    when (renderState) {
                        is RenderState.Ready -> {
                            Text(
                                text = "Dreiecke: ${renderState.trianglesCount} | Punkte: ${renderState.verticesCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Render-Zeit: ${renderState.evaluationTimeMs} ms (${if (renderState.isFullRender) "CGAL F6" else "OpenCSG F5"})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is RenderState.Evaluating -> {
                            Text(
                                text = renderState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        is RenderState.Error -> {
                            Text(
                                text = "Fehler: ${renderState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        RenderState.Idle -> {
                            Text(
                                text = "Bereit für Vorschau (F5) oder Render (F6)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Camera Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            FilledTonalIconButton(
                onClick = {
                    rotationX = 30f
                    rotationY = 45f
                    zoomScale = 1f
                    onResetCamera()
                }
            ) {
                Icon(Icons.Default.CropFree, contentDescription = "Kamera zentrieren")
            }
        }
    }
}
