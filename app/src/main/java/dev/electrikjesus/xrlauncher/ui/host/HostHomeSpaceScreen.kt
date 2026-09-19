package dev.electrikjesus.xrlauncher.ui.host

import android.view.Display
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.HostInputMethod
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.launcher.WorkspaceAppLaunchCoordinator
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTune
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLayoutPresets
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.external.LauncherWorkspaceInteractionLayer
import dev.electrikjesus.xrlauncher.ui.external.rememberDebouncedPanelSaver
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesSpatialWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.glasses.HostXrChromeBar
import dev.electrikjesus.xrlauncher.ui.launcher.rememberLaunchableApps
import dev.electrikjesus.xrlauncher.ui.workspace.LauncherContextMenuHost
import dev.electrikjesus.xrlauncher.ui.workspace.openAppContextMenuFromBounds
import kotlinx.coroutines.launch

/**
 * Expanded / Tier-0 host surface: same GLES Home Space as glasses, with local pointer
 * and a screen-locked top HUD. Settings open as an in-engine dialog (not SettingsActivity).
 */
@Composable
fun HostHomeSpaceScreen(
    launcherPackageName: String,
    workspaceRepository: WorkspaceRepository,
    launchCoordinator: WorkspaceAppLaunchCoordinator,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        GlassesSessionState.hostImmersiveSession = true
        GlassesSessionState.markLauncherForeground()
        onDispose {
            GlassesSessionState.hostImmersiveSession = false
            GlassesSessionState.clearLauncherSession()
            HomeSpaceDialogState.close()
        }
    }

    val launchableApps = rememberLaunchableApps(excludePackageName = launcherPackageName)
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    val panelBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }
    val panels = workspace?.panels ?: Workspace.defaultPanels()
    val pinnedKeys = workspace?.hotseatPins?.toSet() ?: emptySet()
    val hotseatApps = remember(launchableApps, workspace?.hotseatPins) {
        HotseatResolver.resolveHotseatApps(
            apps = launchableApps,
            pinnedKeys = workspace?.hotseatPins ?: emptyList(),
        )
    }
    val panelSaver = rememberDebouncedPanelSaver(workspaceRepository)
    val scope = rememberCoroutineScope()
    val appearance = workspace?.appearance ?: WorkspaceAppearance.default()
    val cursor by CompanionPointerBus.cursor.collectAsState()

    val onLaunchApp: (LaunchableApp) -> Unit = { app ->
        launchCoordinator.launchFromSpatialDesktop(
            app = app,
            visiblePanels = panels.filter { it.visible },
            focusedPanelIndex = CompanionPointerBus.focusedPanelIndex.value,
            displayId = Display.DEFAULT_DISPLAY,
        )
    }
    val onOpenSettings = {
        GlassesSessionState.homeSpaceEdit = false
        HomeSpaceDialogState.openSettings()
    }
    val onToggleHotseatPin: (LaunchableApp) -> Unit = { app ->
        scope.launch { workspaceRepository.toggleHotseatPin(app.componentKey()) }
    }

    val onZoomSphere: (Float) -> Unit = { delta ->
        scope.launch {
            workspaceRepository.nudgeAppearance(HomeSpaceTuneAxis.SPHERE, delta)
        }
    }

    // BumpDesk absolute mouse/touch is the default host path; COMPANION_BUS keeps the
    // older HostPointerBridge for A/B (HostInputMethod.preference).
    val hostContent: @Composable () -> Unit = {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val fallbackWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val fallbackHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
            val measuredWidthPx = with(density) { maxWidth.toPx() }
            val measuredHeightPx = with(density) { maxHeight.toPx() }
            rootWidthPx = if (measuredWidthPx > 1f) measuredWidthPx else fallbackWidthPx
            rootHeightPx = if (measuredHeightPx > 1f) measuredHeightPx else fallbackHeightPx

            LaunchedEffect(rootWidthPx, rootHeightPx) {
                if (rootWidthPx > 1f && rootHeightPx > 1f) {
                    GlassesSessionState.onLauncherRootSized?.invoke()
                }
            }

            val onAppContextMenu = { app: LaunchableApp, bounds: Rect ->
                openAppContextMenuFromBounds(
                    app = app,
                    bounds = bounds,
                    isPinned = app.componentKey() in pinnedKeys,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                GlassesSpatialWorkspaceScreen(
                    launchableApps = launchableApps,
                    hotseatApps = hotseatApps,
                    pinnedComponentKeys = pinnedKeys,
                    panels = panels,
                    appearance = appearance,
                    workspaceRepository = workspaceRepository,
                    onBoundsChanged = { key, rect -> itemBounds[key] = rect },
                    onPanelBoundsChanged = { id, rect -> panelBounds[id] = rect },
                    onPanelsChange = { updatedPanels -> panelSaver.save(updatedPanels) },
                    onLaunchApp = onLaunchApp,
                    onOpenSettings = onOpenSettings,
                    onAppContextMenu = onAppContextMenu,
                    onPanelMinimize = { panelId ->
                        scope.launch { workspaceRepository.setPanelMinimized(panelId, minimized = true) }
                    },
                    onPanelClose = { panelId ->
                        scope.launch { workspaceRepository.setPanelVisible(panelId, visible = false) }
                    },
                    onPanelRestore = { panelId ->
                        scope.launch { workspaceRepository.setPanelMinimized(panelId, minimized = false) }
                    },
                    onCloseEmbedded = { launchCoordinator.closeEmbedded(it) },
                    onPopOutEmbedded = { launchCoordinator.popOutEmbedded(it) },
                    onTuneAppearance = { axis, delta ->
                        scope.launch {
                            workspaceRepository.updateAppearance(HomeSpaceTune.apply(appearance, axis, delta))
                        }
                    },
                    showHostChrome = false,
                    modifier = Modifier.fillMaxSize(),
                )

                LauncherWorkspaceInteractionLayer(
                    launchableApps = launchableApps,
                    panels = panels,
                    pinnedComponentKeys = pinnedKeys,
                    itemBounds = itemBounds,
                    panelBounds = panelBounds,
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    workspaceRepository = workspaceRepository,
                    onLaunchApp = onLaunchApp,
                    onToggleHotseatPin = onToggleHotseatPin,
                    onOpenSettings = onOpenSettings,
                    onOpenAllApps = { GlassesSessionState.showAllAppsOverlay() },
                    onLayoutPresetSelected = { preset ->
                        panelSaver.save(WorkspaceLayoutPresets.apply(panels, preset))
                    },
                    onTuneAppearance = { axis, delta ->
                        scope.launch {
                            workspaceRepository.updateAppearance(HomeSpaceTune.apply(appearance, axis, delta))
                        }
                    },
                    panelScale = appearance.clamped().panelScale,
                    sphereScale = appearance.clamped().sphereScale,
                    showContextMenu = false,
                    onPanelBoundsChanged = { panelId, bounds ->
                        panelSaver.save(
                            panels.map { panel ->
                                if (panel.id == panelId) panel.copy(bounds = bounds) else panel
                            },
                        )
                    },
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (HostInputMethod.preference) {
            HostInputMethod.BUMPDESK -> HostBumpDeskInput(
                modifier = Modifier.fillMaxSize(),
                onZoomSphere = onZoomSphere,
                content = hostContent,
            )
            HostInputMethod.COMPANION_BUS -> HostPointerBridge(
                modifier = Modifier.fillMaxSize(),
                onZoomSphere = onZoomSphere,
                content = hostContent,
            )
        }
        // Wrap-content top HUD only — a fillMaxSize overlay blocked the desk catcher.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .zIndex(8f),
        ) {
            HostXrChromeBar(
                appearance = appearance,
                hoveredLabel = cursor.hoveredLabel,
                workspaceRepository = workspaceRepository,
                onBoundsChanged = { key, rect -> itemBounds[key] = rect },
                onOpenSettings = onOpenSettings,
            )
        }
        val contextMenu by LauncherContextMenuState.request.collectAsState()
        if (contextMenu != null) {
            Box(Modifier.fillMaxSize().zIndex(9f)) {
                LauncherContextMenuHost(
                    rootWidthPx = rootWidthPx,
                    rootHeightPx = rootHeightPx,
                    onLaunchApp = onLaunchApp,
                    onToggleHotseatPin = onToggleHotseatPin,
                    onHidePanel = { panelId ->
                        scope.launch { workspaceRepository.setPanelVisible(panelId, visible = false) }
                    },
                    onSnapPanelToGrid = { panelId ->
                        scope.launch { workspaceRepository.snapPanelToDefaultGrid(panelId) }
                    },
                )
            }
        }
    }
}
