package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.core.workspace.PanelBounds
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
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
    var dragBounds by remember(panel.id, bounds) { mutableStateOf(bounds) }

    val offsetX = (dragBounds.x * containerWidthPx).roundToInt()
    val offsetY = (dragBounds.y * containerHeightPx).roundToInt()
    val widthPx = (dragBounds.width * containerWidthPx).coerceAtLeast(1f)
    val heightPx = (dragBounds.height * containerHeightPx).coerceAtLeast(1f)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(
                width = with(density) { widthPx.toDp() },
                height = with(density) { heightPx.toDp() },
            ),
    ) {
        WorkspacePanelShell(
            panelId = panel.id,
            title = title,
            isFocused = isFocused,
            onPanelBoundsChanged = onPanelBoundsChanged,
            titleBarModifier = Modifier.pointerInput(panel.id, containerWidthPx, containerHeightPx) {
                detectDragGestures(
                    onDragEnd = { onBoundsChanged(dragBounds) },
                    onDragCancel = { dragBounds = bounds },
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
                        .size(20.dp)
                        .background(Color(0xFF03DAC5).copy(alpha = 0.7f), CircleShape)
                        .pointerInput(panel.id, containerWidthPx, containerHeightPx) {
                            detectDragGestures(
                                onDragEnd = { onBoundsChanged(dragBounds) },
                                onDragCancel = { dragBounds = bounds },
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
