package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Dark spatial backdrop tuned for additive glasses optics.
 * [parallaxX]/[parallaxY] are normalized -1..1 from companion cursor for subtle depth.
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
                translationX = parallaxX * 28f
                translationY = parallaxY * 18f
                scaleX = 1.04f
                scaleY = 1.04f
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0221),
                        Color(0xFF1A0A2E),
                        Color(0xFF0B1628),
                    ),
                ),
            ),
    )
}
