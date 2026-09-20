package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntSize
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.HostInputMethod
import dev.electrikjesus.xrlauncher.core.display.GlassesHomeOverlay
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.launcher.GlassesRecentApps
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsOverlayHits
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.electrikjesus.xrlauncher.core.workspace.DeskGroupMoveState
import dev.electrikjesus.xrlauncher.core.workspace.DeskGridOverlay
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconSnapshot
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconTextureBus
import dev.electrikjesus.xrlauncher.core.workspace.DeskPileLayout
import dev.electrikjesus.xrlauncher.core.workspace.DeskPileOps
import dev.electrikjesus.xrlauncher.core.workspace.DeskWidgetResizeState
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialog
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDeskState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.ui.host.HostSettingsDialogLayer
import dev.electrikjesus.xrlauncher.ui.workspace.LocalWorkspaceViewportPx
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.core.workspace.scene.paneKeyPrefix
import dev.electrikjesus.xrlauncher.core.workspace.PerspectiveCursorProbe
import dev.electrikjesus.xrlauncher.core.workspace.PanelBounds
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLayoutPresets
import dev.electrikjesus.xrlauncher.ui.workspace.DraggableWorkspacePanelShell
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.core.workspace.supportsWindowControls
import dev.electrikjesus.xrlauncher.ui.external.ExternalCursorDot
import dev.electrikjesus.xrlauncher.ui.spatial.WorkspaceGlesBackdrop
import dev.electrikjesus.xrlauncher.ui.spatial.gles.DeskIconBitmaps
import dev.electrikjesus.xrlauncher.ui.workspace.EmptySlotPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WidgetPanelById
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.ui.workspace.AllAppsLauncher
import dev.electrikjesus.xrlauncher.ui.workspace.DeskWidgetHostEffect
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceAppDrawerPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceDockShell
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceHotseatRow
import dev.electrikjesus.xrlauncher.ui.workspace.PanelChromeHeader
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspacePanelShell
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceWraparoundLayer
import dev.electrikjesus.xrlauncher.ui.workspace.WraparoundPanelContainer

@Composable
fun GlassesSpatialWorkspaceScreen(
    launchableApps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String> = emptySet(),
    panels: List<PanelState> = Workspace.defaultPanels(),
    onBoundsChanged: (String, Rect) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit = { _, _ -> },
    onPanelsChange: (List<PanelState>) -> Unit = {},
    onLaunchApp: ((LaunchableApp) -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)? = null,
    onPanelMinimize: (String) -> Unit = {},
    onPanelClose: (String) -> Unit = {},
    onPanelRestore: (String) -> Unit = {},
    onCloseEmbedded: (String) -> Unit = {},
    onPopOutEmbedded: (String) -> Unit = {},
    onTuneAppearance: (HomeSpaceTuneAxis, Float) -> Unit = { _, _ -> },
    appearance: WorkspaceAppearance = WorkspaceAppearance.default(),
    workspaceRepository: WorkspaceRepository? = null,
    showHostChrome: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val desktopOverlayActive by DisplayPointerInjector.isAvailableFlow.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(launchableApps, searchQuery) {
        AppRepository.filterLaunchableApps(launchableApps, searchQuery)
    }
    val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()
    val homeOverlay by GlassesSessionState.homeOverlayFlow.collectAsState()
    val editingHomeSpace by GlassesSessionState.homeSpaceEditFlow.collectAsState()
    val hostDialog by HomeSpaceDialogState.dialogFlow.collectAsState()
    val panNorm by GlassesHomeLook.panNormFlow.collectAsState()
    val lookPitch by GlassesHomeLook.lookPitchFlow.collectAsState()
    val lookYawDegrees by GlassesHomeLook.lookYawDegFlow.collectAsState()
    val deskPlaced by HomeSpaceDeskState.placedFlow.collectAsState()
    val deskDrag by HomeSpaceDeskState.dragFlow.collectAsState()
    val deskDrawerPose by HomeSpaceDeskState.drawerPoseFlow.collectAsState()
    val deskPiles by HomeSpaceDeskState.pilesFlow.collectAsState()
    val groupMoveArmed by DeskGroupMoveState.armedKeysFlow.collectAsState()
    val resizeWidgetKey by DeskWidgetResizeState.widgetKeyFlow.collectAsState()
    val appPlanes by GlassesHomeLook.appPlanesFlow.collectAsState()
    val showLayoutPresets by GlassesSessionState.layoutPresetsVisibleFlow.collectAsState()
    val context = LocalContext.current
    var deskHydrated by remember { mutableStateOf(workspaceRepository == null) }

    DeskWidgetHostEffect()

    LaunchedEffect(workspaceRepository) {
        val repo = workspaceRepository ?: run {
            deskHydrated = true
            return@LaunchedEffect
        }
        HomeSpaceDeskState.restore(repo.deskLayout.first())
        deskHydrated = true
    }
    LaunchedEffect(launchableApps) {
        val keys = launchableApps.map { it.componentKey() }.toSet()
        if (HomeSpaceDeskState.pruneMissing(keys) && deskHydrated) {
            workspaceRepository?.saveDeskLayout(HomeSpaceDeskState.toLayout())
        }
    }
    LaunchedEffect(workspaceRepository, deskPlaced, deskDrawerPose, deskDrag, deskPiles, deskHydrated) {
        val repo = workspaceRepository ?: return@LaunchedEffect
        if (!deskHydrated || deskDrag != null) return@LaunchedEffect
        delay(350)
        repo.saveDeskLayout(HomeSpaceDeskState.toLayout())
    }

    LaunchedEffect(launchableApps, deskPlaced, allAppsOverlayVisible, appearance.desktopIcons) {
        val placedKeys = deskPlaced.map { it.app.componentKey }.toSet()
        val unplaced = launchableApps.count { it.componentKey() !in placedKeys }
        // Own pageCount for the GLES desk drawer so Compose grids cannot clamp pager clicks.
        if (allAppsOverlayVisible || appearance.desktopIcons) {
            AllAppsPaginationState.updatePageCount(unplaced, pageSize = HomeSpaceDesk.DRAWER_PAGE_SIZE)
        }
    }
    val visiblePanels = remember(panels) { Workspace.spatialHomePanels(panels) }
    val focusedPanelId by CompanionPointerBus.focusedPanelId.collectAsState()
    val launcherForeground by GlassesSessionState.launcherForegroundFlow.collectAsState()
    val showInAppCursor = !desktopOverlayActive
    val useFreeform = remember(panels) { WorkspaceLayoutPresets.usesFreeformLayout(panels) }
    val inferredPreset = remember(panels) { WorkspaceLayoutPresets.inferPreset(panels) }
    var activePreset by remember(panels) { mutableStateOf(inferredPreset) }

    LaunchedEffect(panels) {
        activePreset = inferredPreset
    }

    LaunchedEffect(panels, visiblePanels) {
        if (CompanionPointerBus.focusedPanelId.value == null && visiblePanels.isNotEmpty()) {
            CompanionPointerBus.setFocusedPanelId(visiblePanels.first().id)
        }
    }

    fun updatePanelBounds(panelId: String, bounds: PanelBounds) {
        onPanelsChange(
            panels.map { panel ->
                if (panel.id == panelId) panel.copy(bounds = bounds.clamp()) else panel
            },
        )
    }

    val tuned = appearance.clamped()
    val panelGapDp = tuned.panelGapDp
    val wrapCurvature = tuned.wrapCurvature
    val workspaceWidth = tuned.workspaceWidth
    val workspaceHeight = tuned.workspaceHeight
    // Live HUD preference wins immediately; appearance catches up via DataStore.
    val lookModePref by GlassesLookMode.preferenceFlow.collectAsState(initial = GlassesLookMode.preference)
    val lookMode = if (launcherForeground) lookModePref else GlassesLookMode.GRADIENT
    val absoluteHostCursor = HostInputMethod.usesAbsoluteHostCursor()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var viewportWidthPx by remember {
        mutableFloatStateOf(with(density) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f))
    }
    var viewportHeightPx by remember {
        mutableFloatStateOf(with(density) { configuration.screenHeightDp.dp.toPx() }.coerceAtLeast(1f))
    }
    val paneArc = HomeSpaceScene.paneArcDegrees(
        viewportWidthPx,
        viewportHeightPx,
        tuned.panelScale,
        tuned.sphereScale,
    )
    GlassesHomeLook.lastPaneArcDegrees = paneArc
    HomeSpaceScene.cursorDeadzoneX = tuned.lookDeadzoneX
    HomeSpaceScene.cursorDeadzoneY = tuned.lookDeadzoneY
    val sceneCamera = HomeSpaceScene.camera(
        look = panNorm,
        cursorX = cursor.x,
        cursorY = cursor.y,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        panelScale = tuned.panelScale,
        sphereScale = tuned.sphereScale,
        lookMode = lookMode,
        lookPitchDeg = lookPitch,
        applyCursorOffset = !absoluteHostCursor,
        lookYawDegrees = lookYawDegrees,
    )
    val homeCamera = WorkspaceCylinderGeometry.CameraState(
        yawDegrees = sceneCamera.yawDeg,
        pitchDegrees = sceneCamera.pitchDeg,
        panNormX = 0f,
        panNormY = 0f,
    )
    val openAllApps = { GlassesSessionState.toggleAllAppsOverlay() }
    val launchApp: (LaunchableApp) -> Unit = { app ->
        GlassesRecentApps.record(app)
        GlassesSessionState.hideHomeOverlays()
        onLaunchApp?.invoke(app)
    }
    val allAppsPage by AllAppsPaginationState.pageIndexFlow.collectAsState()
    // Sync DataStore → preference when appearance changes, but never clobber a newer HUD tap
    // (preference already diverged from the last synced appearance value).
    var lastSyncedLookMode by remember { mutableStateOf<GlassesLookMode?>(null) }
    LaunchedEffect(tuned.lookMode, launcherForeground, absoluteHostCursor) {
        if (launcherForeground) {
            val previous = lastSyncedLookMode
            if (previous == null || GlassesLookMode.preference == previous) {
                GlassesLookMode.preference = tuned.lookMode
            }
            lastSyncedLookMode = tuned.lookMode
        }
        // Companion FPS re-locks to center; absolute host mouse must keep screen coords.
        if (launcherForeground && lookMode == GlassesLookMode.FPS && !absoluteHostCursor) {
            CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
        }
    }
    val densityScale = density.density
    LaunchedEffect(
        tuned.panelScale,
        tuned.sphereScale,
        tuned.desktopIcons,
        tuned.uiScale,
        densityScale,
        launchableApps,
        allAppsOverlayVisible,
        allAppsPage,
        deskPlaced,
        deskDrag,
        deskDrawerPose,
        deskPiles,
        groupMoveArmed,
        resizeWidgetKey,
        viewportWidthPx,
        viewportHeightPx,
    ) {
        if (!tuned.desktopIcons) {
            DeskIconTextureBus.clear()
            return@LaunchedEffect
        }
        val drawerApps = launchableApps.map { app ->
            HomeSpaceDesk.AppRef(
                componentKey = app.componentKey(),
                label = app.label,
                packageName = app.packageName,
            )
        }
        val memberKeys = DeskPileOps.memberKeys(deskPiles)
        val icons = HomeSpaceDesk.layout(
            placed = deskPlaced,
            sphereScale = tuned.sphereScale,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            panelScale = tuned.panelScale,
            uiScale = tuned.uiScale,
            density = densityScale,
            drawerOpen = allAppsOverlayVisible,
            drawerApps = drawerApps.filter { it.componentKey !in memberKeys },
            drawerPage = allAppsPage,
            draggingKey = deskDrag?.app?.componentKey,
            dragYawDeg = deskDrag?.yawDeg ?: 0f,
            dragPitchDeg = deskDrag?.pitchDeg ?: 0f,
            drawerYawDeg = deskDrawerPose?.first,
            drawerPitchDeg = deskDrawerPose?.second ?: 0f,
        )
        val halfW = icons.firstOrNull { it.isDesktopApp }?.halfWidth
            ?: icons.firstOrNull { it.isAppDrawer }?.let { it.halfWidth / HomeSpaceDesk.DRAWER_SCALE }
            ?: HomeSpaceDesk.ICON_HALF_WIDTH
        val halfH = HomeSpaceDesk.labeledIconHalfHeight(halfW)
        DeskGridOverlay.update(
            DeskGridOverlay.config.copy(
                iconHalfWidth = halfW,
                gridScale = tuned.deskGridScale,
                sphereScale = tuned.sphereScale,
                snapToGrid = tuned.deskSnapToGrid,
                showGridOnMove = tuned.deskShowGridOnMove,
            ),
        )
        DeskWidgetResizeState.configure(
            iconHalfWidth = halfW,
            gridScale = tuned.deskGridScale,
            sphereScale = tuned.sphereScale,
            snapToGrid = tuned.deskSnapToGrid,
        )
        val withPiles = DeskPileLayout.appendIcons(
            icons = icons,
            piles = deskPiles,
            sphereScale = tuned.sphereScale,
            halfWidth = halfW,
            halfHeight = halfH,
            draggingKey = deskDrag?.app?.componentKey,
            dragYawDeg = deskDrag?.yawDeg ?: 0f,
            dragPitchDeg = deskDrag?.pitchDeg ?: 0f,
        )
        val withHandle = DeskGroupMoveState.appendHandle(
            icons = withPiles,
            placed = deskPlaced,
            sphereScale = tuned.sphereScale,
            halfWidth = halfW,
        )
        val withResize = DeskWidgetResizeState.appendHandles(
            icons = withHandle,
            placed = deskPlaced,
            sphereScale = tuned.sphereScale,
            handleHalf = halfW * 0.35f,
        )
        // Publish poses immediately so pager/app picks work while bitmaps catch up.
        DeskIconTextureBus.setIcons(withResize)
        DeskIconTextureBus.lastDrawerPage = allAppsPage
        val existing = DeskIconTextureBus.snapshots().associateBy { it.componentKey }
        val missing = withResize.filter { it.componentKey !in existing }
        if (missing.isEmpty()) return@LaunchedEffect
        val created = withContext(Dispatchers.Default) {
            missing.map { icon ->
                DeskIconSnapshot(
                    componentKey = icon.componentKey,
                    bitmap = DeskIconBitmaps.create(context, icon),
                    generation = System.nanoTime(),
                )
            }
        }
        val merged = withResize.map { icon ->
            existing[icon.componentKey] ?: created.first { it.componentKey == icon.componentKey }
        }
        DeskIconTextureBus.set(withResize, merged)
    }
    LaunchedEffect(hotseatApps) {
        GlassesRecentApps.seedIfEmpty(hotseatApps)
    }
    LaunchedEffect(Unit) {
        PerspectiveCursorProbe.requests.collect {
            PerspectiveCursorProbe.play()
        }
    }
    val currentTuned = rememberUpdatedState(tuned)
    val currentAppPlanes = rememberUpdatedState(appPlanes)
    val currentViewportW = rememberUpdatedState(viewportWidthPx)
    val currentViewportH = rememberUpdatedState(viewportHeightPx)
    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrame != 0L && !PerspectiveCursorProbe.playing.value) {
                    val dt = ((now - lastFrame).coerceAtMost(50_000_000L)) / 1_000_000_000f
                    val appearance = currentTuned.value
                    HomeSpaceScene.cursorDeadzoneX = appearance.lookDeadzoneX
                    HomeSpaceScene.cursorDeadzoneY = appearance.lookDeadzoneY
                    GlassesHomeLook.tickEdgePan(CompanionPointerBus.cursor.value.x, dt)
                    val icons = DeskIconTextureBus.icons()
                    val draggingKey = HomeSpaceDeskState.drag?.app?.componentKey
                    val pinned = icons.filter {
                        (it.isAppDrawer || it.isBacking) && it.componentKey != draggingKey
                    }
                    val panes = GlassesHomeLook.deskBlockingSlots(currentAppPlanes.value).map { slot ->
                        HomeSpaceScene.pane(
                            worldX = slot.worldX,
                            viewportWidthPx = currentViewportW.value,
                            viewportHeightPx = currentViewportH.value,
                            panelScale = appearance.panelScale,
                            sphereScale = appearance.sphereScale,
                        )
                    }
                    HomeSpaceDeskState.tickPhysics(
                        dtSec = dt,
                        sphereScale = appearance.sphereScale,
                        uiScale = appearance.uiScale,
                        viewportWidthPx = currentViewportW.value,
                        viewportHeightPx = currentViewportH.value,
                        panelScale = appearance.panelScale,
                        density = densityScale,
                        pinnedObstacles = pinned,
                        panes = panes,
                    )
                }
                lastFrame = now
            }
        }
    }

    val homeSlots = remember(appPlanes) { GlassesHomeLook.homeSpaceSlots(appPlanes) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val overlayViewport = with(LocalDensity.current) {
            IntSize(
                maxWidth.toPx().toInt().coerceAtLeast(1),
                maxHeight.toPx().toInt().coerceAtLeast(1),
            )
        }
        LaunchedEffect(overlayViewport.width, overlayViewport.height) {
            viewportWidthPx = overlayViewport.width.toFloat()
            viewportHeightPx = overlayViewport.height.toFloat()
        }
        WorkspaceGlesBackdrop(
            camera = homeCamera,
            curvature = 1f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
            wallpaperChoice = tuned.wallpaperChoice,
            showWallpaperCylinder = true,
            surroundRoom = true,
            roomRadius = HomeSpaceScene.roomRadius(tuned.sphereScale),
            homeSpaceSlots = homeSlots,
            homeSpacePanelScale = tuned.panelScale,
            homeSpaceSphereScale = tuned.sphereScale,
            homeSpacePanesEnabled = true,
            cursorX = cursor.x,
            cursorY = cursor.y,
            showSphereCursor = true,
            enabled = true,
            modifier = Modifier.fillMaxSize(),
        )

        val prefixBounds: (String) -> ((String, Rect) -> Unit) = { paneId ->
            { key, rect -> onBoundsChanged(HomeSpaceScene.paneKeyPrefix(paneId) + key, rect) }
        }
        GlassesHomeCarousel(
            panNorm = panNorm,
            cursorX = cursor.x,
            cursorY = cursor.y,
            appPlanes = appPlanes,
            uiScale = tuned.uiScale,
            panelScale = tuned.panelScale,
            sphereScale = tuned.sphereScale,
            captureToGles = true,
            onBoundsChanged = onBoundsChanged,
            center = {
                GlassesHomeSpace(
                    hoveredLabel = cursor.hoveredLabel,
                    onBoundsChanged = prefixBounds("home"),
                    onOpenRecents = { GlassesSessionState.toggleHomeOverlay(GlassesHomeOverlay.RECENTS) },
                    onOpenNotifications = { GlassesHomeLook.lookAt(GlassesHomeLook.trayPane()) },
                    onOpenQuickSettings = { GlassesHomeLook.lookAt(GlassesHomeLook.trayPane()) },
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            right = {
                GlassesHomeTrayPane(
                    hoveredLabel = cursor.hoveredLabel,
                    onBoundsChanged = prefixBounds("tray"),
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            appPane = { plane ->
                GlassesAppPlaneLayer(
                    plane = plane,
                    hoveredLabel = cursor.hoveredLabel,
                    onBoundsChanged = prefixBounds(plane.panelId),
                    onClose = { onCloseEmbedded(plane.panelId) },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            modifier = Modifier.fillMaxSize(),
        )

    val recents by GlassesRecentApps.recentsFlow.collectAsState()
    if (homeOverlay == GlassesHomeOverlay.RECENTS && onLaunchApp != null) {
            GlassesRecentsLayer(
                recents = recents,
                hoveredLabel = cursor.hoveredLabel,
                onBoundsChanged = onBoundsChanged,
                onLaunchApp = launchApp,
                onClear = { GlassesRecentApps.clear() },
                onDismiss = { GlassesSessionState.hideHomeOverlays() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        CompositionLocalProvider(
            LocalWorkspaceViewportPx provides overlayViewport,
        ) {
        GlassesHomeTuneOverlay(
            appearance = tuned,
            hoveredLabel = cursor.hoveredLabel,
            editing = editingHomeSpace,
            immersiveDialog = true,
            onBoundsChanged = onBoundsChanged,
            onToggleEdit = { GlassesSessionState.toggleHomeSpaceEdit() },
            onNudge = onTuneAppearance,
        )

        if (hostDialog == HomeSpaceDialog.SETTINGS &&
            workspaceRepository != null &&
            !GlassesSessionState.hostImmersiveSession
        ) {
            HostSettingsDialogLayer(
                workspaceRepository = workspaceRepository,
                hoveredLabel = cursor.hoveredLabel,
                onBoundsChanged = onBoundsChanged,
                onClose = { HomeSpaceDialogState.close() },
            )
        }

        if (showHostChrome) {
            HostXrChromeBar(
                appearance = tuned,
                hoveredLabel = cursor.hoveredLabel,
                workspaceRepository = workspaceRepository,
                onBoundsChanged = onBoundsChanged,
                onOpenSettings = onOpenSettings,
            )
        }
        }

        if (showInAppCursor) {
            ExternalCursorDot(modifier = Modifier.fillMaxSize().zIndex(5f))
        }
    }
}

@Composable
private fun WorkspaceLauncherStatusHints(
    hoveredLabel: String?,
    focusedPanelId: String?,
    launcherForeground: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hoveredLabel == null && (focusedPanelId == null || !launcherForeground)) return

    Column(modifier = modifier) {
        if (hoveredLabel != null) {
            val hoverText = when (hoveredLabel) {
                AllAppsLauncher.HOVER_LABEL -> stringResource(R.string.all_apps)
                AllAppsOverlayHits.CLOSE_HOVER_LABEL -> stringResource(R.string.all_apps_close)
                else -> hoveredLabel
            }
            Text(
                text = stringResource(R.string.cursor_over, hoverText),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (focusedPanelId != null && launcherForeground) {
            Text(
                text = stringResource(
                    R.string.workspace_focused_panel,
                    panelTitleLabel(focusedPanelId),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StackPanelShell(
    panel: PanelState,
    title: String,
    isFocused: Boolean,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    onPanelMinimize: (String) -> Unit,
    onPanelClose: (String) -> Unit,
    onPanelRestore: (String) -> Unit,
    modifier: Modifier = Modifier,
    showChrome: Boolean = true,
    content: @Composable () -> Unit,
) {
    WorkspacePanelShell(
        panelId = panel.id,
        isFocused = isFocused,
        onPanelBoundsChanged = onPanelBoundsChanged,
        modifier = modifier,
        header = if (showChrome) {
            {
                PanelChromeHeader(
                    panelId = panel.id,
                    title = title,
                    isFocused = isFocused,
                    minimized = panel.minimized,
                    showWindowControls = panel.kind.supportsWindowControls(),
                    onPanelBoundsChanged = onPanelBoundsChanged,
                    onMinimize = { onPanelMinimize(panel.id) },
                    onClose = { onPanelClose(panel.id) },
                    onRestore = { onPanelRestore(panel.id) },
                )
            }
        } else {
            null
        },
    ) {
        if (!panel.minimized) {
            content()
        }
    }
}

@Composable
private fun GlassesPanelLayout(
    panels: List<PanelState>,
    launchableApps: List<LaunchableApp>,
    focusedPanelId: String?,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerApps: List<LaunchableApp>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)? = null,
    onOpenAllApps: (() -> Unit)? = null,
    onPanelMinimize: (String) -> Unit = {},
    onPanelClose: (String) -> Unit = {},
    onPanelRestore: (String) -> Unit = {},
    onCloseEmbedded: (String) -> Unit = {},
    onPopOutEmbedded: (String) -> Unit = {},
    allAppsHovered: Boolean = false,
    panelGapDp: Float,
    wrapCurvature: Float,
    workspaceWidth: Float,
    workspaceHeight: Float,
    modifier: Modifier = Modifier,
) {
    val gap = panelGapDp.dp
    val rowSlots = remember(panels) { stackPanelCenters(panels) }
    var slotIndex = 0
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        var index = 0
        while (index < panels.size) {
            val panel = panels[index]
            when (panel.kind) {
                PanelKind.WIDGET -> {
                    val widgetPanels = panels.drop(index).takeWhile { it.kind == PanelKind.WIDGET }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        widgetPanels.forEach { widgetPanel ->
                            val slot = rowSlots[slotIndex++]
                            WraparoundPanelContainer(
                                panelId = widgetPanel.id,
                                centerXNorm = slot.centerX,
                                centerYNorm = slot.centerY,
                                wrapCurvature = wrapCurvature,
                                workspaceWidth = workspaceWidth,
                                workspaceHeight = workspaceHeight,
                                modifier = Modifier.weight(1f),
                            ) {
                                StackPanelShell(
                                    panel = widgetPanel,
                                    title = panelTitle(widgetPanel),
                                    isFocused = focusedPanelId == widgetPanel.id,
                                    onPanelBoundsChanged = onPanelBoundsChanged,
                                    onPanelMinimize = onPanelMinimize,
                                    onPanelClose = onPanelClose,
                                    onPanelRestore = onPanelRestore,
                                    showChrome = widgetPanel.id != "widget_clock",
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    WidgetPanelById(widgetId = widgetPanel.id)
                                }
                            }
                        }
                    }
                    index += widgetPanels.size
                }
                PanelKind.APP_DRAWER -> {
                    val slot = rowSlots[slotIndex++]
                    WraparoundPanelContainer(
                        panelId = panel.id,
                        centerXNorm = slot.centerX,
                        centerYNorm = slot.centerY,
                        wrapCurvature = wrapCurvature,
                        workspaceWidth = workspaceWidth,
                        workspaceHeight = workspaceHeight,
                        modifier = Modifier.weight(1f),
                    ) {
                        StackPanelShell(
                            panel = panel,
                            title = panelTitle(panel),
                            isFocused = focusedPanelId == panel.id,
                            onPanelBoundsChanged = onPanelBoundsChanged,
                            onPanelMinimize = onPanelMinimize,
                            onPanelClose = onPanelClose,
                            onPanelRestore = onPanelRestore,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            WorkspaceAppDrawerPanel(
                                searchQuery = searchQuery,
                                onSearchQueryChange = onSearchQueryChange,
                                apps = drawerApps,
                                hoveredLabel = hoveredLabel,
                                pinnedComponentKeys = pinnedComponentKeys,
                                onBoundsChanged = onBoundsChanged,
                                onLaunchApp = onLaunchApp,
                                onAppContextMenu = onAppContextMenu,
                                useSharedPagination = true,
                            )
                        }
                    }
                    index++
                }
                PanelKind.HOTSEAT -> {
                    Spacer(modifier = Modifier.weight(1f))
                    val slot = rowSlots[slotIndex++]
                    WraparoundPanelContainer(
                        panelId = panel.id,
                        centerXNorm = slot.centerX,
                        centerYNorm = slot.centerY,
                        wrapCurvature = wrapCurvature,
                        workspaceWidth = workspaceWidth,
                        workspaceHeight = workspaceHeight,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        WorkspaceDockShell(
                            panelId = panel.id,
                            isFocused = focusedPanelId == panel.id,
                            onPanelBoundsChanged = onPanelBoundsChanged,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WorkspaceHotseatRow(
                                apps = hotseatApps,
                                hoveredLabel = hoveredLabel,
                                pinnedComponentKeys = pinnedComponentKeys,
                                onBoundsChanged = onBoundsChanged,
                                onLaunchApp = onLaunchApp,
                                onOpenAllApps = onOpenAllApps,
                                onAppContextMenu = onAppContextMenu,
                                allAppsHovered = allAppsHovered,
                            )
                        }
                    }
                    index++
                }
                PanelKind.EMPTY_SLOT -> {
                    val slot = rowSlots[slotIndex++]
                    WraparoundPanelContainer(
                        panelId = panel.id,
                        centerXNorm = slot.centerX,
                        centerYNorm = slot.centerY,
                        wrapCurvature = wrapCurvature,
                        workspaceWidth = workspaceWidth,
                        workspaceHeight = workspaceHeight,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        StackPanelShell(
                            panel = panel,
                            title = panelTitle(panel),
                            isFocused = focusedPanelId == panel.id,
                            onPanelBoundsChanged = onPanelBoundsChanged,
                            onPanelMinimize = onPanelMinimize,
                            onPanelClose = onPanelClose,
                            onPanelRestore = onPanelRestore,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            EmptySlotPanelContent(
                                panel = panel,
                                launchableApps = launchableApps,
                                onCloseEmbedded = onCloseEmbedded,
                                onPopOutEmbedded = onPopOutEmbedded,
                            )
                        }
                    }
                    index++
                }
            }
        }
    }
}

@Composable
private fun FreeformGlassesPanelLayout(
    panels: List<PanelState>,
    launchableApps: List<LaunchableApp>,
    focusedPanelId: String?,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerApps: List<LaunchableApp>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    onPanelFrameChanged: (String, PanelBounds) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)? = null,
    onOpenAllApps: (() -> Unit)? = null,
    onPanelMinimize: (String) -> Unit = {},
    onPanelClose: (String) -> Unit = {},
    onPanelRestore: (String) -> Unit = {},
    onCloseEmbedded: (String) -> Unit = {},
    onPopOutEmbedded: (String) -> Unit = {},
    allAppsHovered: Boolean = false,
    panelGapDp: Float,
    wrapCurvature: Float,
    workspaceWidth: Float,
    workspaceHeight: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        panels.filter { it.bounds != null }.forEach { panel ->
            DraggableWorkspacePanelShell(
                panel = panel,
                title = panelTitle(panel),
                isFocused = focusedPanelId == panel.id,
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                panelGapDp = panelGapDp,
                wrapCurvature = wrapCurvature,
                workspaceWidth = workspaceWidth,
                workspaceHeight = workspaceHeight,
                onBoundsChanged = { bounds -> onPanelFrameChanged(panel.id, bounds) },
                onPanelBoundsChanged = onPanelBoundsChanged,
                onMinimizePanel = { onPanelMinimize(panel.id) },
                onClosePanel = { onPanelClose(panel.id) },
                onRestorePanel = { onPanelRestore(panel.id) },
            ) {
                PanelBody(
                    panel = panel,
                    launchableApps = launchableApps,
                    hotseatApps = hotseatApps,
                    pinnedComponentKeys = pinnedComponentKeys,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    drawerApps = drawerApps,
                    hoveredLabel = hoveredLabel,
                    onBoundsChanged = onBoundsChanged,
                    onLaunchApp = onLaunchApp,
                    onOpenAllApps = onOpenAllApps,
                    onAppContextMenu = onAppContextMenu,
                    allAppsHovered = allAppsHovered,
                    onCloseEmbedded = onCloseEmbedded,
                    onPopOutEmbedded = onPopOutEmbedded,
                )
            }
        }
    }
}

@Composable
private fun PanelBody(
    panel: PanelState,
    launchableApps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerApps: List<LaunchableApp>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)? = null,
    onOpenAllApps: (() -> Unit)? = null,
    onPanelMinimize: (String) -> Unit = {},
    onPanelClose: (String) -> Unit = {},
    onPanelRestore: (String) -> Unit = {},
    allAppsHovered: Boolean = false,
    onCloseEmbedded: (String) -> Unit = {},
    onPopOutEmbedded: (String) -> Unit = {},
) {
    when (panel.kind) {
        PanelKind.WIDGET -> WidgetPanelById(widgetId = panel.id)
        PanelKind.APP_DRAWER -> WorkspaceAppDrawerPanel(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            apps = drawerApps,
            hoveredLabel = hoveredLabel,
            pinnedComponentKeys = pinnedComponentKeys,
            onBoundsChanged = onBoundsChanged,
            onLaunchApp = onLaunchApp,
            onAppContextMenu = onAppContextMenu,
            useSharedPagination = true,
        )
        PanelKind.HOTSEAT -> WorkspaceHotseatRow(
            apps = hotseatApps,
            hoveredLabel = hoveredLabel,
            pinnedComponentKeys = pinnedComponentKeys,
            onBoundsChanged = onBoundsChanged,
            onLaunchApp = onLaunchApp,
            onAppContextMenu = onAppContextMenu,
            onOpenAllApps = onOpenAllApps,
            allAppsHovered = allAppsHovered,
        )
        PanelKind.EMPTY_SLOT -> EmptySlotPanelContent(
            panel = panel,
            launchableApps = launchableApps,
            onCloseEmbedded = onCloseEmbedded,
            onPopOutEmbedded = onPopOutEmbedded,
        )
    }
}

@Composable
private fun EmptySlotPanelContent(
    panel: PanelState,
    launchableApps: List<LaunchableApp>,
    onCloseEmbedded: (String) -> Unit,
    onPopOutEmbedded: (String) -> Unit,
) {
    val hostedLabel = panel.hostedComponentKey?.let { key ->
        launchableApps.find { it.componentKey() == key }?.label ?: key.substringBefore('/')
    }
    EmptySlotPanel(
        hostedAppLabel = hostedLabel,
        onFocusHosted = { CompanionPointerBus.setFocusedPanelId(panel.id) },
        onPopOutHosted = { onPopOutEmbedded(panel.id) },
        onCloseHosted = { onCloseEmbedded(panel.id) },
    )
}

@Composable
private fun panelTitleLabel(panelId: String): String = when (panelId) {
    "widget_clock" -> stringResource(R.string.workspace_panel_clock)
    "widget_calendar" -> stringResource(R.string.workspace_panel_calendar)
    "app_drawer" -> stringResource(R.string.workspace_panel_drawer)
    "hotseat" -> stringResource(R.string.workspace_panel_hotseat)
    "empty_slot" -> stringResource(R.string.workspace_empty_slot)
    else -> panelId
}

@Composable
private fun panelTitle(panel: PanelState): String = panelTitleLabel(panel.id)

private data class StackPanelCenter(
    val centerX: Float,
    val centerY: Float,
)

/** Normalized panel centers for cylindrical wrap in stack layout. */
private fun stackPanelCenters(panels: List<PanelState>): List<StackPanelCenter> {
    val rows = mutableListOf<List<PanelState>>()
    var index = 0
    while (index < panels.size) {
        val panel = panels[index]
        if (panel.kind == PanelKind.WIDGET) {
            val widgets = panels.drop(index).takeWhile { it.kind == PanelKind.WIDGET }
            rows += listOf(widgets)
            index += widgets.size
        } else {
            rows += listOf(panel)
            index++
        }
    }
    val rowCount = rows.size.coerceAtLeast(1)
    return buildList {
        rows.forEachIndexed { rowIndex, rowPanels ->
            val centerY = (rowIndex + 0.5f) / rowCount
            if (rowPanels.size == 1) {
                add(StackPanelCenter(centerX = 0.5f, centerY = centerY))
            } else {
                rowPanels.forEachIndexed { colIndex, _ ->
                    add(
                        StackPanelCenter(
                            centerX = (colIndex + 0.5f) / rowPanels.size,
                            centerY = centerY,
                        ),
                    )
                }
            }
        }
    }
}
