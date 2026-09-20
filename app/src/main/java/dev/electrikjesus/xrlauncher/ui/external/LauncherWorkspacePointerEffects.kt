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
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.HostInputMethod
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.input.bumpdesk.BumpDeskHostGesture
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsOverlayHits
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.AppsPageState
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.launcher.GlassesRecentApps
import dev.electrikjesus.xrlauncher.core.launcher.HomeAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.launcher.LauncherSystemPanels
import dev.electrikjesus.xrlauncher.core.launcher.TrayNotificationBus
import dev.electrikjesus.xrlauncher.core.launcher.paginationStateForPane
import dev.electrikjesus.xrlauncher.core.workspace.DeskGroupMoveState
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconTextureBus
import dev.electrikjesus.xrlauncher.core.workspace.DeskLassoState
import dev.electrikjesus.xrlauncher.core.workspace.DeskPileLayout
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDeskState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTune
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceEditPage
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpacePanePick
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.core.workspace.scene.overlayPx
import dev.electrikjesus.xrlauncher.core.workspace.scene.paneRootKey
import dev.electrikjesus.xrlauncher.core.workspace.scene.pickPane
import dev.electrikjesus.xrlauncher.core.workspace.scene.sphereHit
import dev.electrikjesus.xrlauncher.core.workspace.scene.worldRay
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceTitleBar
import kotlin.math.hypot
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
                    rootWidthPx = rootW,
                    rootHeightPx = rootH,
                    panelScale = currentPanelScale.value,
                    sphereScale = currentSphereScale.value,
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
        bindDeskLeftButtonGrab(rootWidthPx, rootHeightPx, panelScale, sphereScale, apps, itemBounds)
        onDispose {
            CompanionPointerBus.removeClickListener(listener)
            clearDeskLeftButtonGrab()
        }
    }
    // Keep sync Left-down grab params current without tearing down the click listener.
    bindDeskLeftButtonGrab(rootWidthPx, rootHeightPx, panelScale, sphereScale, apps, itemBounds)

    val cursor by CompanionPointerBus.cursor.collectAsState()
    val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()
    val lookPitch by GlassesHomeLook.lookPitchFlow.collectAsState()
    val panNorm by GlassesHomeLook.panNormFlow.collectAsState()
    // Cursor travel drives desk grab. FPS look must not re-enter here (pager chrome),
    // but Hold-Left temporarily unlocks the cursor so drag still receives x/y updates.
    LaunchedEffect(
        cursor.x,
        cursor.y,
        cursor.isPressed,
        rootWidthPx,
        rootHeightPx,
        panelScale,
        sphereScale,
        apps,
        itemBounds,
    ) {
        trackDeskDrag(
            cursor.x,
            cursor.y,
            cursor.isPressed,
            rootWidthPx,
            rootHeightPx,
            panelScale,
            sphereScale,
            apps,
            itemBounds,
        )
    }
    LaunchedEffect(
        cursor.x,
        cursor.y,
        cursor.isPressed,
        itemBounds,
        panelBounds,
        rootWidthPx,
        rootHeightPx,
        apps,
        allAppsOverlayVisible,
        panelScale,
        sphereScale,
        lookPitch,
        panNorm,
    ) {
        val screenPoint = Offset(cursor.x * rootWidthPx, cursor.y * rootHeightPx)
        val pick = homeSpacePick(cursor.x, cursor.y, rootWidthPx, rootHeightPx, panelScale, sphereScale)
        val panePoint = overlayPoint(pick, itemBounds) ?: screenPoint
        val paneBounds = paneItemBounds(itemBounds, pick?.slot?.panelId)
        val homeHover = homeHitKey(screenPoint, screenSpaceBounds(itemBounds))?.let {
            GlassesHomeHits.hoverLabel(it)
        } ?: homeHitKey(panePoint, paneBounds)?.let { GlassesHomeHits.hoverLabel(it) }
        val deskIcon = deskIconAt(
            cursor.x,
            cursor.y,
            rootWidthPx,
            rootHeightPx,
            panelScale,
            sphereScale,
        )
        val hoverLabel = when {
            LauncherContextMenuState.isOpen -> null
            deskIcon != null -> deskIcon.label
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
                AllAppsPageControls.PREV_HOVER
            itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(panePoint) == true ||
                itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(screenPoint) == true ->
                AllAppsPageControls.NEXT_HOVER
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
            pick == null -> HomeSpaceDesk.HOVER_LABEL
            else -> null
        }
        DeskIconTextureBus.setHoveredKey(deskIcon?.componentKey)
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
private var deskGesturePressed = false
/** Empty-desk Hold-Left waits for touch-slop before the lasso stroke (BumpDesk isLassoPending). */
private var deskLassoPending = false
private var pendingLassoCursorX = 0f
private var pendingLassoCursorY = 0f
private var pendingLassoYawDeg = 0f
private var pendingLassoPitchDeg = 0f

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
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
) {
    val app = findAppAt(point, itemBounds, apps)
        ?: deskIconAt(click.x, click.y, rootWidthPx, rootHeightPx, panelScale, sphereScale)
            ?.takeUnless { it.isAppDrawer }
            ?.let { desk -> apps.find { it.componentKey() == desk.componentKey } }
    Log.d(LOG_TAG, "right-click at (${click.x}, ${click.y}) app=${app?.label}")
    val deskHit = deskIconAt(click.x, click.y, rootWidthPx, rootHeightPx, panelScale, sphereScale)
    when {
        app != null -> LauncherContextMenuState.openApp(
            app = app,
            isPinned = app.componentKey() in pinnedComponentKeys,
            anchorX = click.x,
            anchorY = click.y,
        )
        deskHit?.isWidget == true -> {
            DeskLassoState.setSelection(setOf(deskHit.componentKey))
            LauncherContextMenuState.openDesktop(
                anchorX = click.x,
                anchorY = click.y,
                deskYawDeg = deskHit.yawDeg,
                deskPitchDeg = deskHit.pitchDeg,
            )
        }
        else -> {
            val panel = findPanelAt(point, panelBounds, panels)
            if (panel != null) {
                LauncherContextMenuState.openPanel(
                    panelId = panel.id,
                    kind = panel.kind,
                    anchorX = click.x,
                    anchorY = click.y,
                )
            } else if (
                homeSpacePick(click.x, click.y, rootWidthPx, rootHeightPx, panelScale, sphereScale) == null
            ) {
                DeskLassoState.clearSelection()
                val hit = HomeSpaceScene.sphereHit(
                    cursorX = click.x,
                    cursorY = click.y,
                    camera = homeSpaceCamera(click.x, click.y, rootWidthPx, rootHeightPx, panelScale, sphereScale),
                    viewportWidthPx = rootWidthPx,
                    viewportHeightPx = rootHeightPx,
                    sphereScale = sphereScale,
                )
                LauncherContextMenuState.openDesktop(
                    anchorX = click.x,
                    anchorY = click.y,
                    deskYawDeg = hit.yawDeg,
                    deskPitchDeg = hit.pitchDeg,
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
    if (GlassesSessionState.homeSpaceEdit) {
        if (handleHomeSpaceClick(point, screenBounds, onOpenSettings, onOpenAllApps, onTuneAppearance)) return
        if (handleHomeSpaceClick(panePoint, paneBounds, onOpenSettings, onOpenAllApps, onTuneAppearance)) return
    }
    deskIconAt(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)?.let { desk ->
        if (HomeSpaceDeskState.drag?.pulling == true) return
        HomeSpaceDeskState.cancel()
        when {
            desk.isAppDrawer -> {
                Log.d(LOG_TAG, "left-click hit desk all-apps tile")
                GlassesSessionState.toggleAllAppsOverlay()
                return
            }
            desk.isBacking -> return
            desk.isPileBacking -> {
                DeskPileLayout.pileIdFromBackingKey(desk.componentKey)?.let {
                    HomeSpaceDeskState.togglePileExpanded(it)
                }
                return
            }
            desk.isGroupHandle -> return
            desk.isPileFace -> {
                HomeSpaceDeskState.togglePileExpanded(desk.componentKey)
                return
            }
            desk.kind == HomeSpaceDesk.Kind.PAGE_PREV -> {
                AllAppsPaginationState.prevPage()
                return
            }
            desk.kind == HomeSpaceDesk.Kind.PAGE_NEXT -> {
                AllAppsPaginationState.nextPage()
                return
            }
            desk.kind == HomeSpaceDesk.Kind.PAGE -> {
                HomeSpaceDesk.pageIndex(desk.componentKey)?.let { AllAppsPaginationState.goToPage(it) }
                return
            }
        }
        apps.find { it.componentKey() == desk.componentKey }?.let { app ->
            Log.d(LOG_TAG, "left-click hit desk icon=${app.label}")
            GlassesSessionState.hideHomeOverlays()
            DeskLassoState.clearSelection()
            onLaunchApp(app)
            return
        }
    }
    if (handleHomeSpaceClick(point, screenBounds, onOpenSettings, onOpenAllApps, onTuneAppearance)) return
    if (handleHomeSpaceClick(panePoint, paneBounds, onOpenSettings, onOpenAllApps, onTuneAppearance)) return
    val overlayVisible = GlassesSessionState.allAppsOverlayVisible
    // GLES drawer: clicks that land just outside pager chrome still count as "on widget".
    if (
        overlayVisible &&
        openDrawerClickZone(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
    ) {
        Log.d(LOG_TAG, "left-click near open All Apps widget — keep open")
        return
    }
    val hitClose = itemBounds[AllAppsOverlayHits.CLOSE_BOUNDS_KEY]?.containsWithSlop(point) == true
    val hitAllAppsLauncher = itemBounds[AllAppsLauncher.BOUNDS_KEY]?.containsWithSlop(point) == true
    val hitOtherTarget = itemBounds[GlassesWorkspaceTitleBar.BOUNDS_KEY]?.containsWithSlop(point) == true ||
        itemBounds[GlassesWorkspaceTitleBar.LAYOUT_BOUNDS_KEY]?.containsWithSlop(point) == true ||
        isPaginationHit(panePoint, paneBounds, paginationStateForPane(pick?.slot?.panelId)) ||
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
    if (overlayVisible) {
        if (handlePaginationClick(point, screenBounds, AllAppsPaginationState.pages)) return
    }
    if (handlePaginationClick(panePoint, paneBounds, paginationStateForPane(pick?.slot?.panelId))) return
    if (handlePaginationClick(point, itemBounds, AllAppsPaginationState.pages)) return
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
    DeskLassoState.clearSelection()
}

private fun homeHitKey(point: Offset, itemBounds: Map<String, Rect>): String? {
    val contains: (String) -> Boolean = { key ->
        val rect = itemBounds[key]
        rect != null && !rect.isEmpty && rect.containsWithSlop(point)
    }
    GlassesHomeHits.actionKeyAt(contains)?.let { return it }
    return itemBounds.keys.firstOrNull { key ->
        (
            GlassesHomeHits.appClosePanelId(key) != null ||
                key.startsWith(GlassesHomeHits.NOTIFICATION_ITEM_PREFIX)
            ) && contains(key)
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
    // Edit +/- used to stay in the hit map after the card closed, centered on the Home
    // pane. A pagination miss then shrank Icons & elements.
    if (GlassesHomeHits.isEditBodyKey(key) && !GlassesSessionState.homeSpaceEdit) return false
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
        GlassesHomeHits.HUD_SETTINGS -> onOpenSettings()
        GlassesHomeHits.HUD_RECENTER -> {
            CompanionPointerBus.recenterCursor()
            GlassesHomeLook.lookHome()
        }
        // Force modes — never toggle. A delayed LeftClick after Compose already switched
        // would otherwise flip mouse-look back on when the user tapped the eye.
        GlassesHomeHits.HUD_LOOK_MODE -> {
            GlassesLookMode.preference = GlassesLookMode.FPS
            onTuneAppearance(HomeSpaceTuneAxis.LOOK_FPS, 1f)
        }
        GlassesHomeHits.HUD_LOOK_GESTURE -> {
            GlassesLookMode.preference = GlassesLookMode.GESTURE
            onTuneAppearance(HomeSpaceTuneAxis.LOOK_GESTURE, 0f)
        }
        GlassesHomeHits.HUD_INPUT_TOUCHPAD ->
            GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
        GlassesHomeHits.HUD_INPUT_HEAD -> {
            GlassesLookMode.preference = GlassesLookMode.GRADIENT
            onTuneAppearance(HomeSpaceTuneAxis.LOOK_FPS, -1f)
            if (GlassesSessionState.rayNeoUsbAttached) {
                GlassesSessionState.xrInputMode = GlassesXrInputMode.GLASSES_HEAD_TRACKING
                CompanionPointerBus.recenterCursor()
            }
        }
        GlassesHomeHits.HUD_KEYBOARD -> CompanionPointerBus.setTextEntryActive(true)
        GlassesHomeHits.RECENTS_CLEAR -> GlassesRecentApps.clear()
        GlassesHomeHits.NOTIFICATIONS_CLEAR -> TrayNotificationBus.clear()
        GlassesHomeHits.QS_WIFI ->
            GlassesSessionState.appContext?.let { LauncherSystemPanels.openWifi(it) }
        GlassesHomeHits.QS_BLUETOOTH ->
            GlassesSessionState.appContext?.let { LauncherSystemPanels.openBluetooth(it) }
        GlassesHomeHits.QS_BRIGHTNESS ->
            GlassesSessionState.appContext?.let { LauncherSystemPanels.openDisplay(it) }
        GlassesHomeHits.QS_NOTIFICATIONS, GlassesHomeHits.NOTIFICATION_LISTENER ->
            GlassesSessionState.appContext?.let {
                LauncherSystemPanels.openNotificationListenerSettings(it)
            }
        GlassesHomeHits.EDIT_TOGGLE, GlassesHomeHits.EDIT_CLOSE -> {
            HomeSpaceDialogState.close()
            GlassesSessionState.toggleHomeSpaceEdit()
        }
        GlassesHomeHits.EDIT_PANEL_MINUS -> onTuneAppearance(HomeSpaceTuneAxis.PANEL, -HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_PANEL_PLUS -> onTuneAppearance(HomeSpaceTuneAxis.PANEL, HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_SPHERE_MINUS -> onTuneAppearance(HomeSpaceTuneAxis.SPHERE, -HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_SPHERE_PLUS -> onTuneAppearance(HomeSpaceTuneAxis.SPHERE, HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_ELEMENT_MINUS -> onTuneAppearance(HomeSpaceTuneAxis.ELEMENT, -HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_ELEMENT_PLUS -> onTuneAppearance(HomeSpaceTuneAxis.ELEMENT, HomeSpaceTune.STEP)
        GlassesHomeHits.EDIT_PAGE_PERSPECTIVE ->
            GlassesSessionState.homeSpaceEditPage = HomeSpaceEditPage.PERSPECTIVE
        GlassesHomeHits.EDIT_PAGE_DESKTOP ->
            GlassesSessionState.homeSpaceEditPage = HomeSpaceEditPage.DESKTOP
        GlassesHomeHits.EDIT_DESK_ICONS -> onTuneAppearance(HomeSpaceTuneAxis.DESK_ICONS, 0f)
        GlassesHomeHits.EDIT_DESK_PILES -> onTuneAppearance(HomeSpaceTuneAxis.DESK_PILES, 0f)
        GlassesHomeHits.EDIT_DESK_TILES -> onTuneAppearance(HomeSpaceTuneAxis.DESK_TILES, 0f)
        GlassesHomeHits.EDIT_DESK_WIDGETS -> onTuneAppearance(HomeSpaceTuneAxis.DESK_WIDGETS, 0f)
        GlassesHomeHits.EDIT_LOOK_FPS -> onTuneAppearance(HomeSpaceTuneAxis.LOOK_FPS, 0f)
        else -> {
            GlassesHomeHits.notificationKeyFromHit(key)?.let { notifKey ->
                TrayNotificationBus.openKey?.invoke(notifKey)
                return true
            }
            return false
        }
    }
    return true
}

private fun homeSpaceCamera(
    cursorX: Float,
    cursorY: Float,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
): HomeSpaceScene.Camera {
    val arc = HomeSpaceScene.paneArcDegrees(rootWidthPx, rootHeightPx, panelScale, sphereScale)
    GlassesHomeLook.lastPaneArcDegrees = arc
    return HomeSpaceScene.camera(
        look = GlassesHomeLook.panNorm,
        cursorX = cursorX,
        cursorY = cursorY,
        viewportWidthPx = rootWidthPx,
        viewportHeightPx = rootHeightPx,
        panelScale = panelScale,
        sphereScale = sphereScale,
        lookMode = GlassesLookMode.effective(),
        lookPitchDeg = GlassesHomeLook.lookPitch,
        // BumpDesk absolute host: ray through the screen cursor; do not also yaw the camera.
        applyCursorOffset = !HostInputMethod.usesAbsoluteHostCursor(),
        lookYawDegrees = GlassesHomeLook.lookYawDegrees,
    )
}

private fun homeSpacePick(
    cursorX: Float,
    cursorY: Float,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
): HomeSpacePanePick? {
    val camera = homeSpaceCamera(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
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

private fun isPaginationHit(point: Offset, itemBounds: Map<String, Rect>, pages: AppsPageState = AllAppsPaginationState.pages): Boolean {
    if (itemBounds[AllAppsPageControls.PREV_KEY]?.containsWithSlop(point) == true) return true
    if (itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(point) == true) return true
    val pageCount = pages.pageCount
    for (page in 0 until pageCount) {
        if (itemBounds[AllAppsPageControls.pageKey(page)]?.containsWithSlop(point) == true) {
            return true
        }
    }
    return false
}

private fun handlePaginationClick(
    point: Offset,
    itemBounds: Map<String, Rect>,
    pages: AppsPageState,
): Boolean {
    when {
        itemBounds[AllAppsPageControls.PREV_KEY]?.containsWithSlop(point) == true -> {
            Log.d(LOG_TAG, "left-click hit pagination prev panePages=${pages.pageCount}")
            pages.prevPage()
            return true
        }
        itemBounds[AllAppsPageControls.NEXT_KEY]?.containsWithSlop(point) == true -> {
            Log.d(LOG_TAG, "left-click hit pagination next panePages=${pages.pageCount}")
            pages.nextPage()
            return true
        }
        else -> {
            for (page in 0 until pages.pageCount) {
                if (itemBounds[AllAppsPageControls.pageKey(page)]?.containsWithSlop(point) == true) {
                    Log.d(LOG_TAG, "left-click hit pagination page=$page")
                    pages.goToPage(page)
                    return true
                }
            }
        }
    }
    return false
}

/**
 * Host pointer bridge: true when the normalized cursor is over screen-locked HUD / Edit
 * toggle. Desk grab must not start there — Compose clickables own those hits.
 */
fun isHostScreenChromeAt(normX: Float, normY: Float): Boolean {
    if (lastDeskRootWidthPx <= 1f || lastDeskRootHeightPx <= 1f) return false
    val point = Offset(normX * lastDeskRootWidthPx, normY * lastDeskRootHeightPx)
    if (
        lastDeskItemBounds.entries.any { (key, rect) ->
            GlassesHomeHits.isScreenLockedChromeKey(key) &&
                rect.containsWithSlop(point, HOST_CONTROL_HIT_SLOP_PX)
        }
    ) {
        return true
    }
    // Bounds can lag a frame after rotation / first layout. Top-center strip is always HUD.
    if (
        GlassesSessionState.hostImmersiveSession &&
        normY < 0.14f &&
        normX in 0.28f..0.72f
    ) {
        return true
    }
    return false
}

private fun Rect.containsWithSlop(point: Offset, slopPx: Float = CONTROL_HIT_SLOP_PX): Boolean =
    point.x >= left - slopPx && point.x <= right + slopPx &&
        point.y >= top - slopPx && point.y <= bottom + slopPx

private fun deskIconAt(
    cursorX: Float,
    cursorY: Float,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
): HomeSpaceDesk.Icon? {
    val camera = homeSpaceCamera(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
    val ray = HomeSpaceScene.worldRay(
        cursorX = cursorX,
        cursorY = cursorY,
        camera = camera,
        viewportWidthPx = rootWidthPx,
        viewportHeightPx = rootHeightPx,
    )
    val icons = DeskIconTextureBus.icons().ifEmpty {
        HomeSpaceDesk.defaultIcons(sphereScale, rootWidthPx, rootHeightPx, panelScale)
    }
    HomeSpaceDesk.pickAlongRay(ray, icons)?.let { return it }
    // XR companion cursor jitter: magnet to nearest pager. Absolute host mouse must not —
    // a 9° pull launches apps inches away from the arrow.
    if (
        GlassesSessionState.allAppsOverlayVisible &&
        !HostInputMethod.usesAbsoluteHostCursor()
    ) {
        return HomeSpaceDesk.pickNearestPager(ray, icons)
    }
    return null
}

private fun openDrawerClickZone(
    cursorX: Float,
    cursorY: Float,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
): Boolean {
    if (!GlassesSessionState.allAppsOverlayVisible) return false
    val camera = homeSpaceCamera(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
    val ray = HomeSpaceScene.worldRay(
        cursorX = cursorX,
        cursorY = cursorY,
        camera = camera,
        viewportWidthPx = rootWidthPx,
        viewportHeightPx = rootHeightPx,
    )
    val icons = DeskIconTextureBus.icons()
    if (icons.isEmpty()) return false
    return HomeSpaceDesk.inOpenDrawerClickZone(ray, icons)
}

private var lastDeskRootWidthPx = 1920f
private var lastDeskRootHeightPx = 1080f
private var lastDeskPanelScale = 1f
private var lastDeskSphereScale = 1f
private var lastDeskApps: List<LaunchableApp> = emptyList()
private var lastDeskItemBounds: Map<String, Rect> = emptyMap()

/** Sync grab from [CompanionPointerBus.beginLeftButton] while touchpad may already be moving. */
fun bindDeskLeftButtonGrab(
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
    apps: List<LaunchableApp> = emptyList(),
    itemBounds: Map<String, Rect> = emptyMap(),
) {
    lastDeskRootWidthPx = rootWidthPx
    lastDeskRootHeightPx = rootHeightPx
    lastDeskPanelScale = panelScale
    lastDeskSphereScale = sphereScale
    lastDeskApps = apps
    lastDeskItemBounds = itemBounds
    CompanionPointerBus.onLeftButtonDown = {
        val c = CompanionPointerBus.cursor.value
        trackDeskDrag(
            cursorX = c.x,
            cursorY = c.y,
            pressed = true,
            rootWidthPx = lastDeskRootWidthPx,
            rootHeightPx = lastDeskRootHeightPx,
            panelScale = lastDeskPanelScale,
            sphereScale = lastDeskSphereScale,
            apps = lastDeskApps,
            itemBounds = lastDeskItemBounds,
        )
    }
    CompanionPointerBus.onPointerMoveWhilePressed = {
        val c = CompanionPointerBus.cursor.value
        if (c.isPressed) {
            trackDeskDrag(
                cursorX = c.x,
                cursorY = c.y,
                pressed = true,
                rootWidthPx = lastDeskRootWidthPx,
                rootHeightPx = lastDeskRootHeightPx,
                panelScale = lastDeskPanelScale,
                sphereScale = lastDeskSphereScale,
                apps = lastDeskApps,
                itemBounds = lastDeskItemBounds,
            )
        }
    }
    CompanionPointerBus.onPointerGestureFinalize = { endX, endY ->
        // Apply the unlocked end sample, then release — before FPS re-locks to center.
        // Do NOT re-press here: notePointerUp already fired pager/All-Apps chrome, and a
        // second pressed=true would clear hasActiveGesture and grab empty space / dismiss.
        when {
            HomeSpaceDeskState.drag != null || DeskLassoState.active -> {
                trackDeskDrag(
                    cursorX = endX,
                    cursorY = endY,
                    pressed = true,
                    rootWidthPx = lastDeskRootWidthPx,
                    rootHeightPx = lastDeskRootHeightPx,
                    panelScale = lastDeskPanelScale,
                    sphereScale = lastDeskSphereScale,
                    apps = lastDeskApps,
                    itemBounds = lastDeskItemBounds,
                )
                trackDeskDrag(
                    cursorX = endX,
                    cursorY = endY,
                    pressed = false,
                    rootWidthPx = lastDeskRootWidthPx,
                    rootHeightPx = lastDeskRootHeightPx,
                    panelScale = lastDeskPanelScale,
                    sphereScale = lastDeskSphereScale,
                    apps = lastDeskApps,
                    itemBounds = lastDeskItemBounds,
                )
            }
            deskGesturePressed || HomeSpaceDeskState.hasActiveGesture() -> {
                trackDeskDrag(
                    cursorX = endX,
                    cursorY = endY,
                    pressed = false,
                    rootWidthPx = lastDeskRootWidthPx,
                    rootHeightPx = lastDeskRootHeightPx,
                    panelScale = lastDeskPanelScale,
                    sphereScale = lastDeskSphereScale,
                    apps = lastDeskApps,
                    itemBounds = lastDeskItemBounds,
                )
            }
        }
    }
}

fun clearDeskLeftButtonGrab() {
    CompanionPointerBus.onLeftButtonDown = null
    CompanionPointerBus.onPointerMoveWhilePressed = null
    CompanionPointerBus.onPointerGestureFinalize = null
}

/** Second finger / pinch: drop lasso and desk hold without completing a selection. */
fun abortDeskPointerGesture() {
    DeskLassoState.cancel()
    HomeSpaceDeskState.cancel()
    deskGesturePressed = false
    clearPendingLasso()
}

private fun clearPendingLasso() {
    deskLassoPending = false
}

private fun trackDeskDrag(
    cursorX: Float,
    cursorY: Float,
    pressed: Boolean,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
    apps: List<LaunchableApp> = emptyList(),
    itemBounds: Map<String, Rect> = emptyMap(),
) {
    lastDeskRootWidthPx = rootWidthPx
    lastDeskRootHeightPx = rootHeightPx
    lastDeskPanelScale = panelScale
    lastDeskSphereScale = sphereScale
    lastDeskApps = apps
    lastDeskItemBounds = itemBounds
    if (pressed) {
        val camera = homeSpaceCamera(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
        val hit = HomeSpaceScene.sphereHit(
            cursorX = cursorX,
            cursorY = cursorY,
            camera = camera,
            viewportWidthPx = rootWidthPx,
            viewportHeightPx = rootHeightPx,
            sphereScale = sphereScale,
        )
        // Grab on Left-down even if the touchpad finger was already moving (missed rising edge).
        // Gesture look: once a press continues without an icon grab, do not pick up icons
        // crossed mid-pan — that would steal the look gesture.
        val gestureLookIgnoresPathIcons =
            GlassesLookMode.effective() == GlassesLookMode.GESTURE && deskGesturePressed
        if (
            !HomeSpaceDeskState.hasActiveGesture() &&
            !DeskLassoState.active &&
            !deskLassoPending &&
            !gestureLookIgnoresPathIcons
        ) {
            val deskIcon = deskIconAt(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
            when {
                deskIcon != null -> {
                    when {
                        DeskGroupMoveState.canBeginDrag(deskIcon.componentKey) -> {
                            DeskGroupMoveState.beginDrag(
                                componentKey = deskIcon.componentKey,
                                cursorX = cursorX,
                                cursorY = cursorY,
                                grabYawDeg = hit.yawDeg,
                                grabPitchDeg = hit.pitchDeg,
                                placed = HomeSpaceDeskState.placed,
                            )
                        }
                        else -> {
                            // Dragging an item replaces any prior lasso/widget selection.
                            DeskLassoState.clearSelection()
                            HomeSpaceDeskState.press(
                                icon = deskIcon,
                                cursorX = cursorX,
                                cursorY = cursorY,
                                hitYawDeg = hit.yawDeg,
                                hitPitchDeg = hit.pitchDeg,
                            )
                        }
                    }
                }
                tryPressHomePaneApp(
                    cursorX = cursorX,
                    cursorY = cursorY,
                    hitYawDeg = hit.yawDeg,
                    hitPitchDeg = hit.pitchDeg,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    panelScale = panelScale,
                    sphereScale = sphereScale,
                    apps = apps,
                    itemBounds = itemBounds,
                ) -> Unit
                // Near-misses on open All Apps pagination must not start a Desktop lasso —
                // a one-point lasso cancels and the follow-up click dismisses the drawer.
                // Empty desk: pending until touch-slop so long-press can open the radial.
                homeSpacePick(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale) == null &&
                    !openDrawerClickZone(
                        cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale,
                    ) -> {
                    // Empty press dismisses an armed group move (BumpDesk).
                    DeskGroupMoveState.clear()
                    deskLassoPending = true
                    pendingLassoCursorX = cursorX
                    pendingLassoCursorY = cursorY
                    pendingLassoYawDeg = hit.yawDeg
                    pendingLassoPitchDeg = hit.pitchDeg
                }
            }
        }
        if (deskLassoPending && !DeskLassoState.active) {
            val distPx = hypot(
                (cursorX - pendingLassoCursorX) * rootWidthPx,
                (cursorY - pendingLassoCursorY) * rootHeightPx,
            )
            if (distPx > BumpDeskHostGesture.DEFAULT_TOUCH_SLOP_PX) {
                DeskLassoState.begin(pendingLassoYawDeg, pendingLassoPitchDeg)
                clearPendingLasso()
                DeskLassoState.extend(hit.yawDeg, hit.pitchDeg)
            }
        }
        when {
            DeskGroupMoveState.isDragging -> {
                DeskGroupMoveState.move(cursorX, cursorY, hit.yawDeg, hit.pitchDeg)?.let { poses ->
                    HomeSpaceDeskState.applyGroupPoses(poses)
                }
            }
            HomeSpaceDeskState.drag != null ->
                HomeSpaceDeskState.move(cursorX, cursorY, hit.yawDeg, hit.pitchDeg)
            DeskLassoState.active ->
                DeskLassoState.extend(hit.yawDeg, hit.pitchDeg)
        }
    } else if (deskGesturePressed) {
        if (deskLassoPending) {
            clearPendingLasso()
        }
        if (DeskGroupMoveState.isDragging) {
            if (DeskGroupMoveState.endDrag()) {
                // Successful move — drop selection highlight (BumpDesk clears groupSelectedItems).
                DeskLassoState.clearSelection()
            }
        }
        val iconsForLasso = DeskIconTextureBus.icons().ifEmpty {
            HomeSpaceDesk.defaultIcons(sphereScale, rootWidthPx, rootHeightPx, panelScale)
        }
        val captured = DeskLassoState.completePending(iconsForLasso)
        if (captured.isNotEmpty()) {
            val hit = HomeSpaceScene.sphereHit(
                cursorX = cursorX,
                cursorY = cursorY,
                camera = homeSpaceCamera(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale),
                viewportWidthPx = rootWidthPx,
                viewportHeightPx = rootHeightPx,
                sphereScale = sphereScale,
            )
            LauncherContextMenuState.openDesktop(
                anchorX = cursorX,
                anchorY = cursorY,
                deskYawDeg = hit.yawDeg,
                deskPitchDeg = hit.pitchDeg,
            )
        }
        val drag = HomeSpaceDeskState.drag
        val draggingKey = drag?.app?.componentKey
        val icons = iconsForLasso
        val pane = homeSpacePick(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
        val panes = GlassesHomeLook.homeSpaceSlots().map { slot ->
            HomeSpaceScene.pane(
                worldX = slot.worldX,
                viewportWidthPx = rootWidthPx,
                viewportHeightPx = rootHeightPx,
                panelScale = panelScale,
                sphereScale = sphereScale,
            )
        }
        val halfW = icons.firstOrNull { it.componentKey == draggingKey }?.halfWidth
            ?: HomeSpaceDesk.ICON_HALF_WIDTH
        val halfH = icons.firstOrNull { it.componentKey == draggingKey }?.halfHeight
            ?: HomeSpaceDesk.labeledIconHalfHeight(halfW)
        val onDesktop = pane == null
        HomeSpaceDeskState.release(
            onDesktop = onDesktop,
            halfWidth = halfW,
            halfHeight = halfH,
            sphereScale = sphereScale,
            obstacles = icons.filter { it.componentKey != draggingKey },
            panes = panes,
        )
    }
    deskGesturePressed = pressed
}

/** Hold-Left on a Home pane app starts a Desktop copy-drag; Home list is never mutated. */
private fun tryPressHomePaneApp(
    cursorX: Float,
    cursorY: Float,
    hitYawDeg: Float,
    hitPitchDeg: Float,
    rootWidthPx: Float,
    rootHeightPx: Float,
    panelScale: Float,
    sphereScale: Float,
    apps: List<LaunchableApp>,
    itemBounds: Map<String, Rect>,
): Boolean {
    val pick = homeSpacePick(cursorX, cursorY, rootWidthPx, rootHeightPx, panelScale, sphereScale)
        ?: return false
    if (pick.slot.panelId != "home") return false
    val screenPoint = Offset(cursorX * rootWidthPx, cursorY * rootHeightPx)
    val panePoint = overlayPoint(pick, itemBounds) ?: screenPoint
    val paneBounds = paneItemBounds(itemBounds, "home")
    val app = findAppAt(panePoint, paneBounds, apps) ?: return false
    val icon = HomeSpaceDesk.iconOf(
        app = HomeSpaceDesk.AppRef(
            componentKey = app.componentKey(),
            label = app.label,
            packageName = app.packageName,
        ),
        yawDeg = hitYawDeg,
        pitchDeg = hitPitchDeg,
        sphereScale = sphereScale,
        lift = 0.15f,
    )
    HomeSpaceDeskState.press(
        icon = icon,
        cursorX = cursorX,
        cursorY = cursorY,
        hitYawDeg = hitYawDeg,
        hitPitchDeg = hitPitchDeg,
        fromHome = true,
    )
    return true
}

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
    val pageCount = maxOf(AllAppsPaginationState.pages.pageCount, HomeAppsPaginationState.pages.pageCount)
    for (page in 0 until pageCount) {
        if (itemBounds[AllAppsPageControls.pageKey(page)]?.containsWithSlop(point) == true) {
            return AllAppsPageControls.pageHover(page)
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
/** Extra pixels around chrome controls — compensates for cursor/visual offset on glasses. */
private const val CONTROL_HIT_SLOP_PX = 16f
/** Host screen HUD / Edit toggle — slightly larger so desk grab does not steal edge taps. */
private const val HOST_CONTROL_HIT_SLOP_PX = 28f
private const val APP_HIT_SLOP_PX = 8f
