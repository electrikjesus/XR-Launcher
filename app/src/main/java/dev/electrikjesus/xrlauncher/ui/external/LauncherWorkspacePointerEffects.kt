package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceTitleBar
import dev.electrikjesus.xrlauncher.ui.glasses.WorkspaceLayoutPresetBar
import dev.electrikjesus.xrlauncher.ui.workspace.AllAppsLauncher

/** Opens context menus on right-click; left-click launches and dismisses menus on launcher. */
@Composable
fun LauncherWorkspacePointerEffects(
    apps: List<LaunchableApp>,
    panels: List<PanelState>,
    pinnedComponentKeys: Set<String>,
    itemBounds: Map<String, Rect>,
    panelBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllApps: () -> Unit,
    onLayoutPresetSelected: (LayoutPreset) -> Unit,
) {
    val currentApps = rememberUpdatedState(apps)
    val currentPanels = rememberUpdatedState(panels)
    val currentPinned = rememberUpdatedState(pinnedComponentKeys)
    val currentItemBounds = rememberUpdatedState(itemBounds)
    val currentPanelBounds = rememberUpdatedState(panelBounds)
    val currentRootWidthPx = rememberUpdatedState(rootWidthPx)
    val currentRootHeightPx = rememberUpdatedState(rootHeightPx)
    val currentOnLaunchApp = rememberUpdatedState(onLaunchApp)
    val currentOnOpenSettings = rememberUpdatedState(onOpenSettings)
    val currentOnOpenAllApps = rememberUpdatedState(onOpenAllApps)
    val currentOnLayoutPresetSelected = rememberUpdatedState(onLayoutPresetSelected)

    LaunchedEffect(Unit) {
        CompanionPointerBus.clicks.collect { click ->
            val rootW = currentRootWidthPx.value
            val rootH = currentRootHeightPx.value
            val point = Offset(click.x * rootW, click.y * rootH)
            when (click.button) {
                PointerButton.RIGHT -> handleRightClick(
                    click = click,
                    point = point,
                    apps = currentApps.value,
                    panels = currentPanels.value,
                    pinnedComponentKeys = currentPinned.value,
                    itemBounds = currentItemBounds.value,
                    panelBounds = currentPanelBounds.value,
                )
                PointerButton.LEFT -> handleLeftClick(
                    point = point,
                    itemBounds = currentItemBounds.value,
                    onLaunchApp = currentOnLaunchApp.value,
                    onOpenSettings = currentOnOpenSettings.value,
                    onOpenAllApps = currentOnOpenAllApps.value,
                    onLayoutPresetSelected = currentOnLayoutPresetSelected.value,
                    apps = currentApps.value,
                )
            }
        }
    }

    val cursor by CompanionPointerBus.cursor.collectAsState()
    LaunchedEffect(cursor.x, cursor.y, itemBounds, panelBounds, rootWidthPx, rootHeightPx, apps) {
        val point = Offset(cursor.x * rootWidthPx, cursor.y * rootHeightPx)
        val hoverLabel = when {
            LauncherContextMenuState.isOpen -> null
            findAppAt(point, itemBounds, apps)?.label != null ->
                findAppAt(point, itemBounds, apps)?.label
            itemBounds[AllAppsLauncher.BOUNDS_KEY]?.contains(point) == true ->
                AllAppsLauncher.HOVER_LABEL
            itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.contains(point) == true ->
                GlassesWorkspaceTitleBar.HOVER_LABEL
            findPanelAt(point, panelBounds, panels)?.let { panelTitle(it) } != null ->
                findPanelAt(point, panelBounds, panels)?.let { panelTitle(it) }
            else -> null
        }
        CompanionPointerBus.setHoveredLabel(hoverLabel)
    }
}

private fun findAppAt(
    point: Offset,
    itemBounds: Map<String, Rect>,
    apps: List<LaunchableApp>,
): LaunchableApp? {
    val key = itemBounds.entries
        .filter { (id, rect) -> !id.startsWith("__") && rect.contains(point) }
        .minByOrNull { (_, rect) -> rect.width * rect.height }
        ?.key
        ?: return null
    return apps.find { it.componentKey() == key }
}

private fun findPanelAt(
    point: Offset,
    panelBounds: Map<String, Rect>,
    panels: List<PanelState>,
): PanelState? {
    val visibleIds = panels.filter { it.visible }.map { it.id }.toSet()
    val hitId = panelBounds.entries
        .filter { (id, _) -> id in visibleIds }
        .filter { (_, rect) -> rect.contains(point) }
        .minByOrNull { (_, rect) -> rect.width * rect.height }
        ?.key
    return hitId?.let { id -> panels.find { it.id == id } }
}

private fun handleRightClick(
    click: dev.electrikjesus.xrlauncher.core.input.PointerClick,
    point: Offset,
    apps: List<LaunchableApp>,
    panels: List<PanelState>,
    pinnedComponentKeys: Set<String>,
    itemBounds: Map<String, Rect>,
    panelBounds: Map<String, Rect>,
) {
    val app = findAppAt(point, itemBounds, apps)
    Log.d(LOG_TAG, "right-click at (${click.x}, ${click.y}) app=${app?.label}")
    when {
        app != null -> LauncherContextMenuState.openApp(
            app = app,
            isPinned = app.componentKey() in pinnedComponentKeys,
            anchorX = click.x,
            anchorY = click.y,
        )
        else -> {
            val panel = findPanelAt(point, panelBounds, panels)
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

private fun handleLeftClick(
    point: Offset,
    itemBounds: Map<String, Rect>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllApps: () -> Unit,
    onLayoutPresetSelected: (LayoutPreset) -> Unit,
    apps: List<LaunchableApp>,
) {
    if (LauncherContextMenuState.isOpen) {
        LauncherContextMenuState.dismiss()
        return
    }
    findAppAt(point, itemBounds, apps)?.let { app ->
        onLaunchApp(app)
        return
    }
    if (itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.contains(point) == true) {
        onOpenSettings()
        return
    }
    if (itemBounds[AllAppsLauncher.BOUNDS_KEY]?.contains(point) == true) {
        onOpenAllApps()
        return
    }
    LayoutPreset.entries.firstOrNull { preset ->
        itemBounds[WorkspaceLayoutPresetBar.boundsKey(preset)]?.contains(point) == true
    }?.let { preset ->
        onLayoutPresetSelected(preset)
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
