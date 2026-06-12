package dev.electrikjesus.xrlauncher.ui.workspace

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import dev.electrikjesus.xrlauncher.core.workspace.WorkspacePanelTextureBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val CAPTURE_INTERVAL_MS = 120L

@Composable
fun PanelTextureCapture(
    panelId: String,
    centerXNorm: Float,
    centerYNorm: Float,
    enabled: Boolean,
    drawToScreen: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val graphicsLayer = rememberGraphicsLayer()
    var layoutGeneration by remember(panelId) { mutableIntStateOf(0) }
    var widthNorm by remember(panelId) { mutableFloatStateOf(0f) }
    var heightNorm by remember(panelId) { mutableFloatStateOf(0f) }
    val viewport = LocalWorkspaceViewportPx.current

    suspend fun publishCapture() {
        if (widthNorm <= 0f || heightNorm <= 0f) return
        val image = graphicsLayer.toImageBitmap()
        if (image.width <= 0 || image.height <= 0) return
        val androidBitmap = image.asAndroidBitmap()
        val copy = androidBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        WorkspacePanelTextureBus.update(
            panelId = panelId,
            centerXNorm = centerXNorm,
            centerYNorm = centerYNorm,
            widthNorm = widthNorm,
            heightNorm = heightNorm,
            bitmap = copy,
        )
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                val viewportWidth = viewport.width.coerceAtLeast(1)
                val viewportHeight = viewport.height.coerceAtLeast(1)
                val nextWidth = size.width.toFloat() / viewportWidth
                val nextHeight = size.height.toFloat() / viewportHeight
                if (nextWidth != widthNorm || nextHeight != heightNorm) {
                    widthNorm = nextWidth
                    heightNorm = nextHeight
                    layoutGeneration++
                }
            }
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                if (drawToScreen) {
                    drawLayer(graphicsLayer)
                }
            },
    ) {
        content()
    }

    LaunchedEffect(panelId, layoutGeneration, centerXNorm, centerYNorm, enabled) {
        if (!enabled) return@LaunchedEffect
        delay(32)
        publishCapture()
    }

    LaunchedEffect(panelId, enabled) {
        if (!enabled) return@LaunchedEffect
        while (isActive) {
            delay(CAPTURE_INTERVAL_MS)
            publishCapture()
        }
    }

    DisposableEffect(panelId) {
        onDispose { WorkspacePanelTextureBus.remove(panelId) }
    }
}
