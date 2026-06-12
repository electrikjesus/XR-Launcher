package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus

/** Updates focused workspace panel from companion cursor position (glasses path). */
@Composable
fun WorkspacePanelFocusEffects(
    panelIds: List<String>,
    panelBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()

    LaunchedEffect(cursor.x, cursor.y, panelBounds, panelIds, rootWidthPx, rootHeightPx) {
        if (panelIds.isEmpty() || panelBounds.isEmpty()) return@LaunchedEffect
        val point = Offset(cursor.x * rootWidthPx, cursor.y * rootHeightPx)
        val hitPanel = panelBounds.entries
            .filter { (id, _) -> id in panelIds }
            .firstOrNull { (_, rect) -> rect.contains(point) }
            ?.key
        if (hitPanel != null) {
            CompanionPointerBus.setFocusedPanelId(hitPanel)
        }
    }
}
