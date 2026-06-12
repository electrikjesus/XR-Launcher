package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.external.LauncherWorkspacePointerEffects
import dev.electrikjesus.xrlauncher.ui.external.rememberDebouncedPanelSaver
import dev.electrikjesus.xrlauncher.ui.launcher.rememberLaunchableApps

/** Tier 2 / spatial-API path: movable `Subspace` panel wrapping the glasses launcher shell. */
@Composable
fun GlassesWorkspaceScreen(
    launcherPackageName: String,
    workspaceRepository: WorkspaceRepository,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()
    val launchableApps = rememberLaunchableApps(
        excludePackageName = launcherPackageName,
        allAppsOverlayVisible,
    )
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }
    val panels = workspace?.panels ?: Workspace.defaultPanels()
    val panelSaver = rememberDebouncedPanelSaver(workspaceRepository)
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

        LauncherWorkspacePointerEffects(
            apps = launchableApps,
            itemBounds = itemBounds,
            rootWidthPx = rootWidthPx,
            rootHeightPx = rootHeightPx,
            onToggleHotseatPin = onToggleHotseatPin,
        )

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
                            pinnedComponentKeys = workspace?.hotseatPins?.toSet() ?: emptySet(),
                            panels = panels,
                            appearance = workspace?.appearance ?: WorkspaceAppearance.default(),
                            onBoundsChanged = { key, rect -> itemBounds[key] = rect },
                            onPanelsChange = { updated -> panelSaver.save(updated) },
                            onLaunchApp = onLaunchApp,
                        )
                    }
                }
            }
        }
    }
}
