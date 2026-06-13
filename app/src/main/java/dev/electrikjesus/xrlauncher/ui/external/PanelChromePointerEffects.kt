package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
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

    val currentPanels = rememberUpdatedState(panels)
    val currentPanelBounds = rememberUpdatedState(panelBounds)
    val currentRootWidthPx = rememberUpdatedState(rootWidthPx)
    val currentRootHeightPx = rememberUpdatedState(rootHeightPx)
    val currentOnMinimize = rememberUpdatedState(onMinimizePanel)
    val currentOnClose = rememberUpdatedState(onClosePanel)
    val currentOnRestore = rememberUpdatedState(onRestorePanel)

    LaunchedEffect(Unit) {
        CompanionPointerBus.clicks.collect { click ->
            if (click.button != PointerButton.LEFT) return@collect
            val point = Offset(
                click.x * currentRootWidthPx.value,
                click.y * currentRootHeightPx.value,
            )
            val action = chromeActionAt(
                point = point,
                panels = currentPanels.value,
                panelBounds = currentPanelBounds.value,
                onMinimizePanel = currentOnMinimize.value,
                onClosePanel = currentOnClose.value,
                onRestorePanel = currentOnRestore.value,
            ) ?: return@collect
            action()
        }
    }
}

private fun chromeActionAt(
    point: Offset,
    panels: List<PanelState>,
    panelBounds: Map<String, Rect>,
    onMinimizePanel: (String) -> Unit,
    onClosePanel: (String) -> Unit,
    onRestorePanel: (String) -> Unit,
): (() -> Unit)? {
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
