package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.SubspaceSpike
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesSpatialWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.launcher.rememberLaunchableApps

@Composable
fun ExternalDisplayWorkspaceScreen(
    launcherPackageName: String,
    workspaceRepository: WorkspaceRepository,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(GlassesSessionState.preferSubspaceShell, GlassesSessionState.subspaceDecision) {
        SubspaceSpike.logCompositionStage(
            stage = if (GlassesSessionState.preferSubspaceShell) "route_subspace" else "route_flat",
            displayId = GlassesSessionState.secondaryDisplayId,
            decision = GlassesSessionState.subspaceDecision,
        )
    }

    if (GlassesSessionState.preferSubspaceShell) {
        GlassesWorkspaceScreen(
            launcherPackageName = launcherPackageName,
            workspaceRepository = workspaceRepository,
            onLaunchApp = onLaunchApp,
            onToggleHotseatPin = onToggleHotseatPin,
            modifier = modifier,
        )
        return
    }

    FlatGlassesWorkspaceScreen(
        launcherPackageName = launcherPackageName,
        workspaceRepository = workspaceRepository,
        onLaunchApp = onLaunchApp,
        onToggleHotseatPin = onToggleHotseatPin,
        modifier = modifier,
    )
}

@Composable
private fun FlatGlassesWorkspaceScreen(
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
    val panelBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }
    val panels = workspace?.panels ?: Workspace.defaultPanels()
    val visiblePanelIds = remember(panels) { panels.filter { it.visible }.map { it.id } }
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

        WorkspacePanelFocusEffects(
            panelIds = visiblePanelIds,
            panelBounds = panelBounds,
            rootWidthPx = rootWidthPx,
            rootHeightPx = rootHeightPx,
        )

        val panelSaver = rememberDebouncedPanelSaver(workspaceRepository)

        GlassesSpatialWorkspaceScreen(
            launchableApps = launchableApps,
            hotseatApps = hotseatApps,
            pinnedComponentKeys = workspace?.hotseatPins?.toSet() ?: emptySet(),
            panels = panels,
            appearance = workspace?.appearance ?: WorkspaceAppearance.default(),
            onBoundsChanged = { key, rect -> itemBounds[key] = rect },
            onPanelBoundsChanged = { id, rect -> panelBounds[id] = rect },
            onPanelsChange = { updatedPanels -> panelSaver.save(updatedPanels) },
            onLaunchApp = onLaunchApp,
        )
    }
}
