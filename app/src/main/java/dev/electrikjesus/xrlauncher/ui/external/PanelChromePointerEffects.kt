package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.supportsWindowControls
import dev.electrikjesus.xrlauncher.ui.workspace.PanelChromeBounds

/** Companion pointer clicks on panel minimize / close / restore chrome buttons. */
@Composable
fun PanelChromePointerEffects(
    panels: List<PanelState>,
    panelBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onMinimizePanel: (String) -> Unit,
    onClosePanel: (String) -> Unit,
    onRestorePanel: (String) -> Unit,
) {
    if (!GlassesSessionState.launcherForeground) return

    fun point(x: Float, y: Float): Offset = Offset(x * rootWidthPx, y * rootHeightPx)

    fun chromeActionAt(point: Offset): (() -> Unit)? {
        panels.filter { it.visible && it.kind.supportsWindowControls() }.forEach { panel ->
            panelBounds[PanelChromeBounds.closeKey(panel.id)]?.takeIf { it.contains(point) }?.let {
                return { onClosePanel(panel.id) }
            }
            if (panel.minimized) {
                panelBounds[PanelChromeBounds.restoreKey(panel.id)]?.takeIf { it.contains(point) }?.let {
                    return { onRestorePanel(panel.id) }
                }
            } else {
                panelBounds[PanelChromeBounds.minimizeKey(panel.id)]?.takeIf { it.contains(point) }?.let {
                    return { onMinimizePanel(panel.id) }
                }
            }
        }
        return null
    }

    LaunchedEffect(panels, panelBounds, rootWidthPx, rootHeightPx) {
        CompanionPointerBus.clicks.collect { click ->
            val action = chromeActionAt(point(click.x, click.y)) ?: return@collect
            action()
        }
    }
}
