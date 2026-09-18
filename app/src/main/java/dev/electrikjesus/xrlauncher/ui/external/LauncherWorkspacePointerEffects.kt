package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceTitleBar
import dev.electrikjesus.xrlauncher.ui.glasses.WorkspaceLayoutPresetBar
import dev.electrikjesus.xrlauncher.ui.workspace.AllAppsLauncher
import dev.electrikjesus.xrlauncher.ui.workspace.AllAppsPageControls

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

    DisposableEffect(Unit) {
        val listener: (dev.electrikjesus.xrlauncher.core.input.PointerClick) -> Unit = { click ->
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
                PointerButton.LEFT -> {
                    Log.d(
                        LOG_TAG,
                        "left-click norm=(${click.x}, ${click.y}) px=(${point.x}, ${point.y}) " +
                            "root=${rootW.toInt()}x${rootH.toInt()} bounds=${currentItemBounds.value.size}",
                    )
                    handleLeftClick(
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
        CompanionPointerBus.addClickListener(listener)
        onDispose { CompanionPointerBus.removeClickListener(listener) }
    }

    val cursor by CompanionPointerBus.cursor.collectAsState()
    LaunchedEffect(cursor.x, cursor.y, itemBounds, panelBounds, rootWidthPx, rootHeightPx, apps) {
        val point = Offset(cursor.x * rootWidthPx, cursor.y * rootHeightPx)
        val hoverLabel = when {
            LauncherContextMenuState.isOpen -> null
            itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.containsWithSlop(point) == true ->
                GlassesWorkspaceTitleBar.HOVER_LABEL
            itemBounds[GlassesWorkspaceTitleBar.LAYOUT_BOUNDS_KEY]?.containsWithSlop(point) == true ->
                GlassesWorkspaceTitleBar.LAYOUT_HOVER_LABEL
            itemBounds[AllAppsLauncher.BOUNDS_KEY]?.containsWithSlop(point) == true ->
                AllAppsLauncher.HOVER_LABEL
            itemBounds[AllAppsPageControls.PREV_KEY]?.containsWithSlop(point) == true ->
                PAGE_PREV_HOVER
            itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(point) == true ->
                PAGE_NEXT_HOVER
            paginationPageHoverLabel(point, itemBounds) != null ->
                paginationPageHoverLabel(point, itemBounds)
            layoutPresetHoverLabel(point, itemBounds) != null ->
                layoutPresetHoverLabel(point, itemBounds)
            findAppAt(point, itemBounds, apps)?.label != null ->
                findAppAt(point, itemBounds, apps)?.label
            findPanelAt(point, panelBounds, panels)?.let { panelTitle(it) } != null ->
                findPanelAt(point, panelBounds, panels)?.let { panelTitle(it) }
            else -> null
        }
        if (hoverLabel != lastLoggedHoverLabel) {
            Log.d(
                LOG_TAG,
                "hover label=$hoverLabel norm=(${cursor.x}, ${cursor.y}) px=(${point.x}, ${point.y}) " +
                    "pageCount=${AllAppsPaginationState.pageCountFlow.value}",
            )
            lastLoggedHoverLabel = hoverLabel
        }
        CompanionPointerBus.setHoveredLabel(hoverLabel)
    }
}

private var lastLoggedHoverLabel: String? = null

private fun findAppAt(
    point: Offset,
    itemBounds: Map<String, Rect>,
    apps: List<LaunchableApp>,
): LaunchableApp? {
    val key = itemBounds.entries
        .filter { (id, rect) -> !id.startsWith("__") && rect.containsWithSlop(point, APP_HIT_SLOP_PX) }
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
        Log.d(LOG_TAG, "left-click dismiss context menu")
        LauncherContextMenuState.dismiss()
        return
    }
    if (itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.containsWithSlop(point) == true) {
        Log.d(LOG_TAG, "left-click hit settings")
        onOpenSettings()
        return
    }
    if (itemBounds[GlassesWorkspaceTitleBar.LAYOUT_BOUNDS_KEY]?.containsWithSlop(point) == true) {
        Log.d(LOG_TAG, "left-click hit layout orbiter")
        GlassesSessionState.toggleLayoutPresets()
        return
    }
    if (itemBounds[AllAppsLauncher.BOUNDS_KEY]?.containsWithSlop(point) == true) {
        Log.d(LOG_TAG, "left-click hit all-apps launcher")
        onOpenAllApps()
        return
    }
    if (handlePaginationClick(point, itemBounds)) return
    LayoutPreset.entries.firstOrNull { preset ->
        itemBounds[WorkspaceLayoutPresetBar.boundsKey(preset)]?.containsWithSlop(point) == true
    }?.let { preset ->
        Log.d(LOG_TAG, "left-click hit layout preset=$preset")
        onLayoutPresetSelected(preset)
        return
    }
    findAppAt(point, itemBounds, apps)?.let { app ->
        Log.d(LOG_TAG, "left-click hit app=${app.label}")
        onLaunchApp(app)
        return
    }
    logClickMiss(point, itemBounds)
}

private fun handlePaginationClick(point: Offset, itemBounds: Map<String, Rect>): Boolean {
    when {
        itemBounds[AllAppsPageControls.PREV_KEY]?.containsWithSlop(point) == true -> {
            Log.d(LOG_TAG, "left-click hit pagination prev")
            AllAppsPaginationState.prevPage()
            return true
        }
        itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(point) == true -> {
            Log.d(LOG_TAG, "left-click hit pagination next")
            AllAppsPaginationState.nextPage()
            return true
        }
        else -> {
            val pageCount = AllAppsPaginationState.pageCountFlow.value
            for (page in 0 until pageCount) {
                if (itemBounds[AllAppsPageControls.pageKey(page)]?.containsWithSlop(point) == true) {
                    Log.d(LOG_TAG, "left-click hit pagination page=$page")
                    AllAppsPaginationState.goToPage(page)
                    return true
                }
            }
        }
    }
    return false
}

private fun Rect.containsWithSlop(point: Offset, slopPx: Float = CONTROL_HIT_SLOP_PX): Boolean =
    point.x >= left - slopPx && point.x <= right + slopPx &&
        point.y >= top - slopPx && point.y <= bottom + slopPx

private fun logClickMiss(point: Offset, itemBounds: Map<String, Rect>) {
    val paginationKeys = buildList {
        add(AllAppsPageControls.PREV_KEY)
        add(AllAppsPageControls.NEXT_KEY)
        repeat(AllAppsPaginationState.pageCountFlow.value) { add(AllAppsPageControls.pageKey(it)) }
    }
    val nearest = (itemBounds.keys.filter { it in paginationKeys } + LayoutPreset.entries.map {
        WorkspaceLayoutPresetBar.boundsKey(it)
    }).mapNotNull { key ->
        itemBounds[key]?.let { rect -> key to rect }
    }.minByOrNull { (_, rect) ->
        val cx = rect.center.x
        val cy = rect.center.y
        (point.x - cx) * (point.x - cx) + (point.y - cy) * (point.y - cy)
    }
    Log.d(
        LOG_TAG,
        "left-click miss px=(${point.x}, ${point.y}) nearest=${nearest?.first} " +
            "rect=${nearest?.second} paginationKeys=${paginationKeys.filter { itemBounds.containsKey(it) }}",
    )
}

private fun layoutPresetHoverLabel(point: Offset, itemBounds: Map<String, Rect>): String? =
    LayoutPreset.entries.firstOrNull { preset ->
        itemBounds[WorkspaceLayoutPresetBar.boundsKey(preset)]?.containsWithSlop(point) == true
    }?.let { presetHoverName(it) }

private fun paginationPageHoverLabel(point: Offset, itemBounds: Map<String, Rect>): String? {
    val pageCount = AllAppsPaginationState.pageCountFlow.value
    for (page in 0 until pageCount) {
        if (itemBounds[AllAppsPageControls.pageKey(page)]?.containsWithSlop(point) == true) {
            return "Page ${page + 1}"
        }
    }
    return null
}

private fun presetHoverName(preset: LayoutPreset): String = when (preset) {
    LayoutPreset.STANDARD -> "Standard layout"
    LayoutPreset.SINGLE -> "Single layout"
    LayoutPreset.DUAL -> "Dual layout"
    LayoutPreset.TRIPTYCH -> "Triptych layout"
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
private const val PAGE_PREV_HOVER = "Previous page"
private const val PAGE_NEXT_HOVER = "Next page"
/** Extra pixels around chrome controls — compensates for cursor/visual offset on glasses. */
private const val CONTROL_HIT_SLOP_PX = 16f
private const val APP_HIT_SLOP_PX = 8f
