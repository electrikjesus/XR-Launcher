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
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@Composable
fun DraggableWorkspacePanelShell(
    panel: PanelState,
    title: String,
    isFocused: Boolean,
    containerWidthPx: Float,
    containerHeightPx: Float,
    panelGapDp: Float = 12f,
    wrapCurvature: Float = WorkspaceAppearance.DEFAULT_WRAP_CURVATURE,
    workspaceWidth: Float = 1f,
    workspaceHeight: Float = 1f,
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
    val gapPx = with(density) { panelGapDp.dp.toPx() }
    val halfGap = (gapPx / 2f).roundToInt()
    val elevation = if (isFocused) 18.dp else 10.dp
    val centerX = dragBounds.x + dragBounds.width / 2f
    val centerY = dragBounds.y + dragBounds.height / 2f

    WraparoundPanelContainer(
        centerXNorm = centerX,
        centerYNorm = centerY,
        wrapCurvature = wrapCurvature,
        workspaceWidth = workspaceWidth,
        workspaceHeight = workspaceHeight,
        applyArcPositionShift = false,
        modifier = modifier
            .offset { IntOffset(offsetX + halfGap, offsetY + halfGap) }
            .size(
                width = with(density) { (widthPx - gapPx).coerceAtLeast(1f).toDp() },
                height = with(density) { (heightPx - gapPx).coerceAtLeast(1f).toDp() },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
}

private val MaterialGlassShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
