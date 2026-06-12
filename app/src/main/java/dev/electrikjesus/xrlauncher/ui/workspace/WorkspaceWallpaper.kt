package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Immersive twilight environment inspired by Android XR home/recents —
 * layered silhouettes with cursor parallax (2.5D on Tier 1 EXTERNAL display).
 */
@Composable
fun WorkspaceWallpaper(
    parallaxX: Float = 0f,
    parallaxY: Float = 0f,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1.08f
                scaleY = 1.08f
            },
    ) {
        // Sky — moves slowest for depth.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX * 18f
                    translationY = parallaxY * 10f
                },
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1026),
                        Color(0xFF1B2A52),
                        Color(0xFF5B3A6B),
                        Color(0xFFE8784A),
                        Color(0xFFF4A261),
                    ),
                    startY = 0f,
                    endY = size.height * 0.72f,
                ),
                size = size,
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D1B33).copy(alpha = 0.85f),
                        Color(0xFF1A1210),
                    ),
                    startY = size.height * 0.55f,
                    endY = size.height,
                ),
                size = size,
            )
        }

        // Distant mesas.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX * 32f
                    translationY = parallaxY * 14f
                },
        ) {
            drawTerrainLayer(
                baseY = size.height * 0.58f,
                color = Color(0xFF3D2244),
                peaks = listOf(0.08f, 0.22f, 0.38f, 0.55f, 0.72f, 0.88f),
                heights = listOf(0.12f, 0.18f, 0.10f, 0.22f, 0.14f, 0.16f),
            )
        }

        // Mid formations.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX * 48f
                    translationY = parallaxY * 20f
                },
        ) {
            drawTerrainLayer(
                baseY = size.height * 0.68f,
                color = Color(0xFF5C2E35),
                peaks = listOf(0.05f, 0.18f, 0.32f, 0.48f, 0.65f, 0.82f, 0.95f),
                heights = listOf(0.16f, 0.24f, 0.14f, 0.28f, 0.20f, 0.26f, 0.18f),
            )
        }

        // Foreground ridge + ground.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX * 64f
                    translationY = parallaxY * 26f
                },
        ) {
            drawTerrainLayer(
                baseY = size.height * 0.78f,
                color = Color(0xFF2A1518),
                peaks = listOf(0.0f, 0.15f, 0.35f, 0.52f, 0.70f, 0.85f, 1.0f),
                heights = listOf(0.22f, 0.30f, 0.18f, 0.34f, 0.26f, 0.32f, 0.24f),
            )
            drawRect(
                color = Color(0xFF120A0C),
                topLeft = Offset(0f, size.height * 0.88f),
                size = Size(size.width, size.height * 0.12f),
            )
        }

        // Soft vignette.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.45f),
                    ),
                    center = Offset(size.width / 2f, size.height * 0.45f),
                    radius = size.maxDimension * 0.85f,
                ),
                size = size,
            )
        }
    }
}

private fun DrawScope.drawTerrainLayer(
    baseY: Float,
    color: Color,
    peaks: List<Float>,
    heights: List<Float>,
) {
    if (peaks.isEmpty()) return
    val path = Path().apply {
        moveTo(0f, size.height)
        moveTo(0f, baseY - size.height * heights.first())
        peaks.zip(heights).forEach { (xNorm, hNorm) ->
            lineTo(size.width * xNorm, baseY - size.height * hNorm)
        }
        lineTo(size.width, baseY - size.height * heights.last())
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}
