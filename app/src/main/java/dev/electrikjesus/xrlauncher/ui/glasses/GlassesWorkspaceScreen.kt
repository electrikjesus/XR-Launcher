package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLayoutPresets
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.external.LauncherWorkspaceInteractionLayer
import dev.electrikjesus.xrlauncher.ui.external.rememberDebouncedPanelSaver
import dev.electrikjesus.xrlauncher.ui.launcher.rememberLaunchableApps
import dev.electrikjesus.xrlauncher.ui.workspace.openAppContextMenuFromBounds

/** Tier 2 / spatial-API path: movable `Subspace` panel wrapping the glasses launcher shell. */
@Composable
fun GlassesWorkspaceScreen(
    launcherPackageName: String,
    workspaceRepository: WorkspaceRepository,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    onCloseEmbedded: (String) -> Unit = {},
    onPopOutEmbedded: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()
    val launchableApps = rememberLaunchableApps(
        excludePackageName = launcherPackageName,
        allAppsOverlayVisible,
    )
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    val panelBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }
    val panels = workspace?.panels ?: Workspace.defaultPanels()
    val pinnedKeys = workspace?.hotseatPins?.toSet() ?: emptySet()
    val panelSaver = rememberDebouncedPanelSaver(workspaceRepository)
    val context = LocalContext.current
    val hotseatApps = remember(launchableApps, workspace?.hotseatPins) {
        HotseatResolver.resolveHotseatApps(
            apps = launchableApps,
            pinnedKeys = workspace?.hotseatPins ?: emptyList(),
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        rootWidthPx = with(density) { maxWidth.toPx() }
        rootHeightPx = with(density) { maxHeight.toPx() }

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
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                color = Color.Black,
            ) {
                SubspaceSpikeProbe {
                    Subspace {
                        SubspaceInnerSpikeMarker(stage = "subspace_root")
                        SpatialPanel(
                            modifier = SubspaceModifier
                                .width(960.dp)
                                .height(540.dp)
                                .movable()
                                .resizable(),
                        ) {
                            SubspaceInnerSpikeMarker(stage = "spatial_panel")
                            GlassesSpatialWorkspaceScreen(
                                launchableApps = launchableApps,
                                hotseatApps = hotseatApps,
                                pinnedComponentKeys = pinnedKeys,
                                panels = panels,
                                appearance = workspace?.appearance ?: WorkspaceAppearance.default(),
                                workspaceRepository = workspaceRepository,
                                onBoundsChanged = { key, rect -> itemBounds[key] = rect },
                                onPanelBoundsChanged = { id, rect -> panelBounds[id] = rect },
                                onPanelsChange = { updated -> panelSaver.save(updated) },
                                onLaunchApp = onLaunchApp,
                                onOpenSettings = { DisplayLaunchHelper.openSettings(context) },
                                onAppContextMenu = onAppContextMenu,
                                onCloseEmbedded = onCloseEmbedded,
                                onPopOutEmbedded = onPopOutEmbedded,
                            )
                        }
                    }
                }
            }

            val appearance = workspace?.appearance ?: WorkspaceAppearance.default()
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
                onOpenSettings = { DisplayLaunchHelper.openSettings(context) },
                onOpenAllApps = { GlassesSessionState.showAllAppsOverlay() },
                onLayoutPresetSelected = { preset ->
                    panelSaver.save(WorkspaceLayoutPresets.apply(panels, preset))
                },
                onPanelBoundsChanged = { panelId, bounds ->
                    panelSaver.save(
                        panels.map { panel ->
                            if (panel.id == panelId) panel.copy(bounds = bounds) else panel
                        },
                    )
                },
                panelScale = appearance.clamped().panelScale,
                sphereScale = appearance.clamped().sphereScale,
            )
        }
    }
}
