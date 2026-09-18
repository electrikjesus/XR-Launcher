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
import dev.electrikjesus.xrlauncher.core.display.GlassesHomeOverlay
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsOverlayHits
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.launcher.GlassesRecentApps
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTune
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpacePanePick
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.core.workspace.scene.overlayPx
import dev.electrikjesus.xrlauncher.core.workspace.scene.paneRootKey
import dev.electrikjesus.xrlauncher.core.workspace.scene.pickPane
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
    onTuneAppearance: (HomeSpaceTuneAxis, Float) -> Unit = { _, _ -> },
    panelScale: Float = 1f,
    sphereScale: Float = 1f,
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
    val currentOnTuneAppearance = rememberUpdatedState(onTuneAppearance)
    val currentPanelScale = rememberUpdatedState(panelScale)
    val currentSphereScale = rememberUpdatedState(sphereScale)

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
                        cursorX = click.x,
                        cursorY = click.y,
                        itemBounds = currentItemBounds.value,
                        onLaunchApp = currentOnLaunchApp.value,
                        onOpenSettings = currentOnOpenSettings.value,
                        onOpenAllApps = currentOnOpenAllApps.value,
                        onLayoutPresetSelected = currentOnLayoutPresetSelected.value,
                        onTuneAppearance = currentOnTuneAppearance.value,
                        apps = currentApps.value,
                        rootWidthPx = rootW,
                        rootHeightPx = rootH,
                        panelScale = currentPanelScale.value,
                        sphereScale = currentSphereScale.value,
                    )
                }
            }
        }
        CompanionPointerBus.addClickListener(listener)
        onDispose { CompanionPointerBus.removeClickListener(listener) }
    }

    val cursor by CompanionPointerBus.cursor.collectAsState()
    val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()
    LaunchedEffect(
        cursor.x,
        cursor.y,
        itemBounds,
        panelBounds,
        rootWidthPx,
        rootHeightPx,
        apps,
        allAppsOverlayVisible,
        panelScale,
        sphereScale,
    ) {
        val screenPoint = Offset(cursor.x * rootWidthPx, cursor.y * rootHeightPx)
        val pick = homeSpacePick(cursor.x, cursor.y, rootWidthPx, rootHeightPx, panelScale, sphereScale)
        val panePoint = overlayPoint(pick, itemBounds) ?: screenPoint
        val paneBounds = paneItemBounds(itemBounds, pick?.slot?.panelId)
        val homeHover = homeHitKey(screenPoint, screenSpaceBounds(itemBounds))?.let {
            GlassesHomeHits.hoverLabel(it)
        } ?: homeHitKey(panePoint, paneBounds)?.let { GlassesHomeHits.hoverLabel(it) }
        val hoverLabel = when {
            LauncherContextMenuState.isOpen -> null
            homeHover != null -> homeHover
            allAppsOverlayVisible &&
                itemBounds[AllAppsOverlayHits.CLOSE_BOUNDS_KEY]?.containsWithSlop(screenPoint) == true ->
                AllAppsOverlayHits.CLOSE_HOVER_LABEL
            itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.containsWithSlop(screenPoint) == true ->
                GlassesWorkspaceTitleBar.HOVER_LABEL
            itemBounds[GlassesWorkspaceTitleBar.LAYOUT_BOUNDS_KEY]?.containsWithSlop(screenPoint) == true ->
                GlassesWorkspaceTitleBar.LAYOUT_HOVER_LABEL
            itemBounds[AllAppsLauncher.BOUNDS_KEY]?.containsWithSlop(screenPoint) == true ->
                AllAppsLauncher.HOVER_LABEL
            itemBounds[AllAppsPageControls.PREV_KEY]?.containsWithSlop(panePoint) == true ||
                itemBounds[AllAppsPageControls.PREV_KEY]?.containsWithSlop(screenPoint) == true ->
                PAGE_PREV_HOVER
            itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(panePoint) == true ||
                itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(screenPoint) == true ->
                PAGE_NEXT_HOVER
            paginationPageHoverLabel(panePoint, paneBounds) != null ->
                paginationPageHoverLabel(panePoint, paneBounds)
            paginationPageHoverLabel(screenPoint, itemBounds) != null ->
                paginationPageHoverLabel(screenPoint, itemBounds)
            layoutPresetHoverLabel(screenPoint, itemBounds) != null ->
                layoutPresetHoverLabel(screenPoint, itemBounds)
            findAppAt(panePoint, paneBounds, apps)?.label != null ->
                findAppAt(panePoint, paneBounds, apps)?.label
            findAppAt(screenPoint, screenSpaceBounds(itemBounds), apps)?.label != null ->
                findAppAt(screenPoint, screenSpaceBounds(itemBounds), apps)?.label
            findPanelAt(screenPoint, panelBounds, panels)?.let { panelTitle(it) } != null ->
                findPanelAt(screenPoint, panelBounds, panels)?.let { panelTitle(it) }
            else -> null
        }
        if (hoverLabel != lastLoggedHoverLabel) {
            Log.d(
                LOG_TAG,
                "hover label=$hoverLabel norm=(${cursor.x}, ${cursor.y}) px=(${screenPoint.x}, ${screenPoint.y}) " +
                    "pane=${pick?.slot?.panelId} uv=${pick?.u},${pick?.v} " +
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
    cursorX: Float,
    cursorY: Float,
    itemBounds: Map<String, Rect>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllApps: () -> Unit,
    onLayoutPresetSelected: (LayoutPreset) -> Unit,
    onTuneAppearance: (HomeSpaceTuneAxis, Float) -> Unit,
    apps: List<LaunchableApp>,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
) {
    if (LauncherContextMenuState.isOpen) {
        Log.d(LOG_TAG, "left-click dismiss context menu")
        LauncherContextMenuState.dismiss()
        return
    }
    val pick = homeSpacePick(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
    val panePoint = overlayPoint(pick, itemBounds) ?: point
    val paneBounds = paneItemBounds(itemBounds, pick?.slot?.panelId)
    val screenBounds = screenSpaceBounds(itemBounds)
    if (handleHomeSpaceClick(point, screenBounds, onOpenSettings, onOpenAllApps, onTuneAppearance)) return
    if (handleHomeSpaceClick(panePoint, paneBounds, onOpenSettings, onOpenAllApps, onTuneAppearance)) return
    val overlayVisible = GlassesSessionState.allAppsOverlayVisible
    val hitClose = itemBounds[AllAppsOverlayHits.CLOSE_BOUNDS_KEY]?.containsWithSlop(point) == true
    val hitAllAppsLauncher = itemBounds[AllAppsLauncher.BOUNDS_KEY]?.containsWithSlop(point) == true
    val hitOtherTarget = itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.containsWithSlop(point) == true ||
        itemBounds[GlassesWorkspaceTitleBar.LAYOUT_BOUNDS_KEY]?.containsWithSlop(point) == true ||
        isPaginationHit(panePoint, paneBounds) ||
        isPaginationHit(point, itemBounds) ||
        LayoutPreset.entries.any { preset ->
            itemBounds[WorkspaceLayoutPresetBar.boundsKey(preset)]?.containsWithSlop(point) == true
        } ||
        findAppAt(panePoint, paneBounds, apps) != null
    if (
        AllAppsOverlayHits.shouldDismiss(
            overlayVisible = overlayVisible,
            hitClose = hitClose,
            hitOtherTarget = hitOtherTarget,
        )
    ) {
        Log.d(LOG_TAG, "left-click dismiss all-apps overlay close=$hitClose launcher=$hitAllAppsLauncher")
        GlassesSessionState.hideAllAppsOverlay()
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
    if (hitAllAppsLauncher) {
        Log.d(LOG_TAG, "left-click hit all-apps launcher")
        onOpenAllApps()
        return
    }
    if (handlePaginationClick(panePoint, paneBounds)) return
    if (handlePaginationClick(point, itemBounds)) return
    LayoutPreset.entries.firstOrNull { preset ->
        itemBounds[WorkspaceLayoutPresetBar.boundsKey(preset)]?.containsWithSlop(point) == true
    }?.let { preset ->
        Log.d(LOG_TAG, "left-click hit layout preset=$preset")
        onLayoutPresetSelected(preset)
        return
    }
    findAppAt(panePoint, paneBounds, apps)?.let { app ->
        Log.d(LOG_TAG, "left-click hit app=${app.label} pane=${pick?.slot?.panelId}")
        GlassesSessionState.hideHomeOverlays()
        onLaunchApp(app)
        return
    }
    logClickMiss(point, itemBounds)
}

private fun homeHitKey(point: Offset, itemBounds: Map<String, Rect>): String? {
    val contains: (String) -> Boolean = { key ->
        itemBounds[key]?.containsWithSlop(point) == true
    }
    GlassesHomeHits.actionKeyAt(contains)?.let { return it }
    return itemBounds.keys.firstOrNull { key ->
        GlassesHomeHits.appClosePanelId(key) != null && contains(key)
    }
}

private fun handleHomeSpaceClick(
    point: Offset,
    itemBounds: Map<String, Rect>,
    onOpenSettings: () -> Unit,
    onOpenAllApps: () -> Unit,
    onTuneAppearance: (HomeSpaceTuneAxis, Float) -> Unit = { _, _ -> },
): Boolean {
    val key = homeHitKey(point, itemBounds) ?: return false
    Log.d(LOG_TAG, "left-click hit home chrome=$key")
    GlassesHomeHits.appClosePanelId(key)?.let { panelId ->
        GlassesHomeLook.closeAppPlane(panelId)
        GlassesSessionState.panelEmbedRegistry?.closeEmbedded(panelId)
        return true
    }
    when (key) {
        GlassesHomeHits.HOME, GlassesHomeHits.OVERLAY_CLOSE -> {
            GlassesSessionState.hideHomeOverlays()
            GlassesHomeLook.lookHome()
        }
        GlassesHomeHits.ALL_APPS -> onOpenAllApps()
        GlassesHomeHits.RECENTS ->
            GlassesSessionState.toggleHomeOverlay(GlassesHomeOverlay.RECENTS)
        GlassesHomeHits.NOTIFICATIONS, GlassesHomeHits.QUICK_SETTINGS ->
            GlassesHomeLook.lookAt(GlassesHomeLook.trayPane())
        GlassesHomeHits.SETTINGS -> onOpenSettings()
        GlassesHomeHits.RECENTS_CLEAR -> GlassesRecentApps.clear()
        GlassesHomeHits.NOTIFICATIONS_CLEAR -> { }
        GlassesHomeHits.EDIT_TOGGLE -> GlassesSessionState.toggleHomeSpaceEdit()
        GlassesHomeHits.EDIT_PANEL_MINUS -> onTuneAppearance(HomeSpaceTuneAxis.PANEL, -HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_PANEL_PLUS -> onTuneAppearance(HomeSpaceTuneAxis.PANEL, HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_SPHERE_MINUS -> onTuneAppearance(HomeSpaceTuneAxis.SPHERE, -HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_SPHERE_PLUS -> onTuneAppearance(HomeSpaceTuneAxis.SPHERE, HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_ELEMENT_MINUS -> onTuneAppearance(HomeSpaceTuneAxis.ELEMENT, -HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_ELEMENT_PLUS -> onTuneAppearance(HomeSpaceTuneAxis.ELEMENT, HomeSpaceTune.STEP)
        else -> return false
    }
    return true
}

private fun homeSpacePick(
    cursorX: Float,
    cursorY: Float,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
): HomeSpacePanePick? {
    val camera = HomeSpaceScene.camera(
        look = GlassesHomeLook.panNorm,
        cursorX = 0.5f,
        cursorY = 0.5f,
        viewportWidthPx = rootWidthPx,
        viewportHeightPx = rootHeightPx,
        panelScale = panelScale,
    )
    return HomeSpaceScene.pickPane(
        cursorX = cursorX,
        cursorY = cursorY,
        camera = camera,
        slots = GlassesHomeLook.homeSpaceSlots(),
        viewportWidthPx = rootWidthPx,
        viewportHeightPx = rootHeightPx,
        panelScale = panelScale,
        sphereScale = sphereScale,
    )
}

private fun overlayPoint(
    pick: HomeSpacePanePick?,
    itemBounds: Map<String, Rect>,
): Offset? {
    pick ?: return null
    val root = itemBounds[HomeSpaceScene.paneRootKey(pick.slot.panelId)] ?: return null
    val (x, y) = HomeSpaceScene.overlayPx(pick, root.left, root.top, root.width, root.height)
    return Offset(x, y)
}

private fun screenSpaceBounds(itemBounds: Map<String, Rect>): Map<String, Rect> =
    itemBounds.filterKeys { key -> !key.contains("::") && !key.startsWith("__xr_pane_root_") }

private fun paneItemBounds(itemBounds: Map<String, Rect>, paneId: String?): Map<String, Rect> {
    if (paneId == null) return emptyMap()
    val prefix = "$paneId::"
    return itemBounds
        .filterKeys { it.startsWith(prefix) }
        .mapKeys { it.key.removePrefix(prefix) }
}

private fun isPaginationHit(point: Offset, itemBounds: Map<String, Rect>): Boolean {
    if (itemBounds[AllAppsPageControls.PREV_KEY]?.containsWithSlop(point) == true) return true
    if (itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(point) == true) return true
    val pageCount = AllAppsPaginationState.pageCountFlow.value
    for (page in 0 until pageCount) {
        if (itemBounds[AllAppsPageControls.pageKey(page)]?.containsWithSlop(point) == true) {
            return true
        }
    }
    return false
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
