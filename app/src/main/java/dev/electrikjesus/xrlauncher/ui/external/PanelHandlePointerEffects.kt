package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.workspace.PanelBounds
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGrid

private sealed class PanelHandleInteraction {
    abstract val panelId: String

    data class Move(override val panelId: String) : PanelHandleInteraction()
    data class Resize(override val panelId: String) : PanelHandleInteraction()
}

/**
 * Companion pointer drag on panel move/resize handles (works with accessibility inject).
 */
@Composable
fun PanelHandlePointerEffects(
    panels: List<PanelState>,
    panelBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onPanelBoundsChanged: (String, PanelBounds) -> Unit,
) {
    val launcherForeground by GlassesSessionState.launcherForegroundFlow.collectAsState()
    if (!launcherForeground) return

    var activeInteraction by remember { mutableStateOf<PanelHandleInteraction?>(null) }
    var pressAnchor by remember { mutableStateOf<Offset?>(null) }
    var pressBounds by remember { mutableStateOf<PanelBounds?>(null) }

    fun cursorPoint(x: Float, y: Float): Offset =
        Offset(x * rootWidthPx, y * rootHeightPx)

    fun findHandleAt(point: Offset): PanelHandleInteraction? {
        panelBounds.entries
            .filter { (key, _) -> key.endsWith("__resize_handle__") }
            .firstOrNull { (_, rect) -> rect.contains(point) }
            ?.key
            ?.removeSuffix("__resize_handle__")
            ?.let { return PanelHandleInteraction.Resize(it) }

        panelBounds.entries
            .filter { (key, _) -> key.endsWith("__drag_handle__") }
            .firstOrNull { (_, rect) -> rect.contains(point) }
            ?.key
            ?.removeSuffix("__drag_handle__")
            ?.let { return PanelHandleInteraction.Move(it) }

        return null
    }

    LaunchedEffect(panels, panelBounds, rootWidthPx, rootHeightPx) {
        var wasPressed = false
        CompanionPointerBus.cursor.collect { cursor ->
            if (!GlassesSessionState.launcherForeground) {
                activeInteraction = null
                pressAnchor = null
                pressBounds = null
                wasPressed = false
                return@collect
            }
            val point = cursorPoint(cursor.x, cursor.y)
            if (cursor.isPressed && !wasPressed) {
                activeInteraction = findHandleAt(point)
                pressAnchor = point
                pressBounds = activeInteraction?.panelId?.let { id ->
                    panels.find { it.id == id }?.bounds
                }
            }
            val interaction = activeInteraction
            val anchor = pressAnchor
            val startBounds = pressBounds
            if (cursor.isPressed && interaction != null && anchor != null && startBounds != null) {
                val deltaX = (point.x - anchor.x) / rootWidthPx
                val deltaY = (point.y - anchor.y) / rootHeightPx
                val updated = when (interaction) {
                    is PanelHandleInteraction.Move -> startBounds.copy(
                        x = startBounds.x + deltaX,
                        y = startBounds.y + deltaY,
                    )
                    is PanelHandleInteraction.Resize -> startBounds.copy(
                        width = startBounds.width + deltaX,
                        height = startBounds.height + deltaY,
                    )
                }.clamp()
                onPanelBoundsChanged(interaction.panelId, updated)
            }
            if (!cursor.isPressed && wasPressed && interaction != null) {
                panels.find { it.id == interaction.panelId }?.bounds?.let { bounds ->
                    onPanelBoundsChanged(
                        interaction.panelId,
                        WorkspaceCylinderGrid.snapBounds(bounds).clamp(),
                    )
                }
                activeInteraction = null
                pressAnchor = null
                pressBounds = null
            }
            wasPressed = cursor.isPressed
        }
    }
}

private fun String.removeSuffix(suffix: String): String =
    if (endsWith(suffix)) substring(0, length - suffix.length) else this
