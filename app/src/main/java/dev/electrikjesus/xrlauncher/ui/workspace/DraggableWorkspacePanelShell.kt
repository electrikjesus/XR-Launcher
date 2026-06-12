package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.core.workspace.PanelBounds
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@Composable
fun DraggableWorkspacePanelShell(
    panel: PanelState,
    title: String,
    isFocused: Boolean,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onBoundsChanged: (PanelBounds) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bounds = panel.bounds ?: return
    val density = LocalDensity.current
    var dragBounds by remember(panel.id) { mutableStateOf(bounds) }
    val isDragging = remember(panel.id) { AtomicBoolean(false) }

    LaunchedEffect(bounds) {
        if (!isDragging.get()) {
            dragBounds = bounds
        }
    }

    val offsetX = (dragBounds.x * containerWidthPx).roundToInt()
    val offsetY = (dragBounds.y * containerHeightPx).roundToInt()
    val widthPx = (dragBounds.width * containerWidthPx).coerceAtLeast(1f)
    val heightPx = (dragBounds.height * containerHeightPx).coerceAtLeast(1f)
    val elevation = if (isFocused) 18.dp else 10.dp

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(
                width = with(density) { widthPx.toDp() },
                height = with(density) { heightPx.toDp() },
            )
            .graphicsLayer {
                val scale = if (isFocused) 1.02f else 1f
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, MaterialGlassShape),
    ) {
        WorkspacePanelShell(
            panelId = panel.id,
            title = title,
            isFocused = isFocused,
            onPanelBoundsChanged = onPanelBoundsChanged,
            titleBarModifier = Modifier.pointerInput(panel.id, containerWidthPx, containerHeightPx) {
                detectDragGestures(
                    onDragStart = { isDragging.set(true) },
                    onDragEnd = {
                        isDragging.set(false)
                        onBoundsChanged(dragBounds)
                    },
                    onDragCancel = {
                        isDragging.set(false)
                        dragBounds = bounds
                    },
                ) { _, dragAmount ->
                    dragBounds = dragBounds.copy(
                        x = dragBounds.x + dragAmount.x / containerWidthPx,
                        y = dragBounds.y + dragAmount.y / containerHeightPx,
                    ).clamp()
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFF03DAC5).copy(alpha = 0.85f), CircleShape)
                        .pointerInput(panel.id, containerWidthPx, containerHeightPx) {
                            detectDragGestures(
                                onDragStart = { isDragging.set(true) },
                                onDragEnd = {
                                    isDragging.set(false)
                                    onBoundsChanged(dragBounds)
                                },
                                onDragCancel = {
                                    isDragging.set(false)
                                    dragBounds = bounds
                                },
                            ) { _, dragAmount ->
                                dragBounds = dragBounds.copy(
                                    width = dragBounds.width + dragAmount.x / containerWidthPx,
                                    height = dragBounds.height + dragAmount.y / containerHeightPx,
                                ).clamp()
                            }
                        },
                )
            }
        }
    }
}

private val MaterialGlassShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
