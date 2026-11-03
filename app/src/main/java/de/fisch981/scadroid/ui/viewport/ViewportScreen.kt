package de.fisch981.scadroid.ui.viewport

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.max
import de.fisch981.scadroid.engine.ScadGlRenderer
import de.fisch981.scadroid.engine.TriangleMesh
import de.fisch981.scadroid.model.RenderState
import de.fisch981.scadroid.ui.theme.DarkBorder
import de.fisch981.scadroid.ui.theme.DarkSurface
import de.fisch981.scadroid.ui.theme.ScadGold

@Composable
fun ViewportScreen(
    renderState: RenderState,
    mesh: TriangleMesh,
    modifier: Modifier = Modifier
) {
    val renderer = remember { ScadGlRenderer() }

    // Aktualisiere das gerenderte Mesh bei Änderungen
    LaunchedEffect(mesh) {
        renderer.currentMesh = mesh
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101216))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        val count = pressedChanges.size

                        if (count == 1) {
                            // 1 Finger: Drehung (Rotation um Zielpunkt / Drehpunkt)
                            val change = pressedChanges[0]
                            val delta = change.position - change.previousPosition
                            if (delta.x != 0f || delta.y != 0f) {
                                renderer.yaw = (renderer.yaw + delta.x * 0.35f) % 360f
                                renderer.pitch = (renderer.pitch + delta.y * 0.25f).coerceIn(-85f, 85f)
                                change.consume()
                            }
                        } else if (count >= 2) {
                            // 2 Finger: Drehpunkt per Touch verschieben (Pan) & Pinch-to-Zoom
                            val p1 = pressedChanges[0].position
                            val p2 = pressedChanges[1].position
                            val prev1 = pressedChanges[0].previousPosition
                            val prev2 = pressedChanges[1].previousPosition

                            // 1. Zoom
                            val currentDist = (p1 - p2).getDistance()
                            val prevDist = (prev1 - prev2).getDistance()
                            if (prevDist > 1f && currentDist > 1f) {
                                val zoomRatio = currentDist / prevDist
                                if (zoomRatio > 0f) {
                                    renderer.distance = (renderer.distance / zoomRatio).coerceIn(10f, 600f)
                                }
                            }

                            // 2. Pan: Mittelpunkt der 2 Finger verschiebt den Drehpunkt
                            val currentCenter = (p1 + p2) / 2f
                            val prevCenter = (prev1 + prev2) / 2f
                            val panDelta = currentCenter - prevCenter
                            if (panDelta.x != 0f || panDelta.y != 0f) {
                                renderer.panCamera(panDelta.x, panDelta.y)
                            }

                            pressedChanges.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        // Native OpenGL ES 3.0 Surface
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    setRenderer(renderer)
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
                colors = CardDefaults.cardColors(containerColor = Color(0xDD181A20)),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2D35))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = ScadGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "3D VIEWPORT (OpenGL ES 3.0)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScadGold,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.size(4.dp))

                    when (renderState) {
                        is RenderState.Ready -> {
                            if (mesh.triangleCount == 0) {
                                Text(
                                    text = "Modell: Keine Geometrie (Leer)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB0B0B0)
                                )
                            } else {
                                Text(
                                    text = "Modell: ${mesh.triangleCount} Dreiecke | ${mesh.vertexCount} Vertices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "Abmessungen: ${(mesh.maxBound.x - mesh.minBound.x).toInt()} × ${(mesh.maxBound.y - mesh.minBound.y).toInt()} × ${(mesh.maxBound.z - mesh.minBound.z).toInt()} mm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB0B0B0)
                                )
                            }
                            Text(
                                text = "Auswertung: ${renderState.evaluationTimeMs} ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF81C784)
                            )
                        }
                        is RenderState.Evaluating -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = ScadGold
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = renderState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ScadGold
                                )
                            }
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
                                text = "Tippe oben auf Vorschau oder Render",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }
                }
            }
        }

        // Kamera-Schnellwahltasten (Rechts Unten)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Isometric Standardansicht
            SmallFloatingActionButton(
                onClick = {
                    renderer.yaw = 45f
                    renderer.pitch = 30f
                    renderer.distance = max(mesh.boundingRadius * 2.8f, 40f)
                    renderer.panX = 0f
                    renderer.panY = 0f
                },
                containerColor = Color(0xFF242730),
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text("3D", style = MaterialTheme.typography.labelMedium)
            }

            // Draufsicht (Top)
            SmallFloatingActionButton(
                onClick = {
                    renderer.yaw = 0f
                    renderer.pitch = 89.9f
                    renderer.distance = max(mesh.boundingRadius * 2.5f, 40f)
                },
                containerColor = Color(0xFF242730),
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text("Top", style = MaterialTheme.typography.labelMedium)
            }

            // Fit / Reset Center
            SmallFloatingActionButton(
                onClick = {
                    renderer.resetCamera(mesh)
                },
                containerColor = ScadGold,
                contentColor = Color(0xFF121212)
            ) {
                Icon(Icons.Default.FitScreen, contentDescription = "Modell zentrieren")
            }
        }
    }
}
