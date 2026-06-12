package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceTitleBar
import dev.electrikjesus.xrlauncher.ui.workspace.AllAppsLauncher

/** Opens context menus on right-click; updates hover labels for launcher hit targets. */
@Composable
fun LauncherWorkspacePointerEffects(
    apps: List<LaunchableApp>,
    panels: List<PanelState>,
    pinnedComponentKeys: Set<String>,
    itemBounds: Map<String, Rect>,
    panelBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
) {
    fun cursorPoint(normalizedX: Float, normalizedY: Float): Offset =
        Offset(normalizedX * rootWidthPx, normalizedY * rootHeightPx)

    fun findAppAt(point: Offset): LaunchableApp? {
        val key = itemBounds.entries.firstOrNull { (_, rect) -> rect.contains(point) }?.key
        return key?.let { k -> apps.find { it.componentName.flattenToString() == k } }
    }

    fun findPanelAt(point: Offset): PanelState? {
        val visibleIds = panels.filter { it.visible }.map { it.id }.toSet()
        val hitId = panelBounds.entries
            .filter { (id, _) -> id in visibleIds }
            .filter { (_, rect) -> rect.contains(point) }
            .minByOrNull { (_, rect) -> rect.width * rect.height }
            ?.key
        return hitId?.let { id -> panels.find { it.id == id } }
    }

    LaunchedEffect(Unit) {
        CompanionPointerBus.clicks.collect { click ->
            if (click.button != PointerButton.RIGHT) return@collect
            val point = cursorPoint(click.x, click.y)
            val app = findAppAt(point)
            Log.d(
                LOG_TAG,
                "right-click at (${click.x}, ${click.y}) app=${app?.label}",
            )
            when {
                app != null -> LauncherContextMenuState.openApp(
                    app = app,
                    isPinned = app.componentKey() in pinnedComponentKeys,
                    anchorX = click.x,
                    anchorY = click.y,
                )
                else -> {
                    val panel = findPanelAt(point)
                    if (panel != null) {
                        LauncherContextMenuState.openPanel(
                            panelId = panel.id,
                            kind = panel.kind,
                            anchorX = click.x,
                            anchorY = click.y,
                        )
                    }
                }
            }
        }
    }

    val cursor by CompanionPointerBus.cursor.collectAsState()
    LaunchedEffect(cursor.x, cursor.y, itemBounds.size, panelBounds.size, rootWidthPx, rootHeightPx) {
        val point = cursorPoint(cursor.x, cursor.y)
        val hoverLabel = when {
            LauncherContextMenuState.isOpen -> null
            findAppAt(point)?.label != null -> findAppAt(point)?.label
            itemBounds[AllAppsLauncher.BOUNDS_KEY]?.contains(point) == true ->
                AllAppsLauncher.HOVER_LABEL
            itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.contains(point) == true ->
                GlassesWorkspaceTitleBar.HOVER_LABEL
            findPanelAt(point)?.let { panelTitle(it) } != null -> findPanelAt(point)?.let { panelTitle(it) }
            else -> null
        }
        CompanionPointerBus.setHoveredLabel(hoverLabel)
    }
}

private fun panelTitle(panel: PanelState): String = when (panel.id) {
    "widget_clock" -> "Clock"
    "widget_calendar" -> "Calendar"
    "app_drawer" -> "Apps"
    "hotseat" -> "Hotseat"
    else -> when (panel.kind) {
        PanelKind.WIDGET -> panel.id
        PanelKind.APP_DRAWER -> "Apps"
        PanelKind.HOTSEAT -> "Hotseat"
        PanelKind.EMPTY_SLOT -> "Empty slot"
    }
}

private const val LOG_TAG = "XRLauncher/Pointer"
