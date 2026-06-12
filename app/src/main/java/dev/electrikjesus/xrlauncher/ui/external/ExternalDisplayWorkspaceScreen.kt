package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesSpatialWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceScreen

@Composable
fun ExternalDisplayWorkspaceScreen(
    apps: List<LaunchableApp>,
    workspaceRepository: WorkspaceRepository,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (GlassesSessionState.preferSubspaceShell) {
        GlassesWorkspaceScreen(
            apps = apps,
            workspaceRepository = workspaceRepository,
            onLaunchApp = onLaunchApp,
            onToggleHotseatPin = onToggleHotseatPin,
            modifier = modifier,
        )
        return
    }

    FlatGlassesWorkspaceScreen(
        apps = apps,
        workspaceRepository = workspaceRepository,
        onLaunchApp = onLaunchApp,
        onToggleHotseatPin = onToggleHotseatPin,
        modifier = modifier,
    )
}

@Composable
private fun FlatGlassesWorkspaceScreen(
    apps: List<LaunchableApp>,
    workspaceRepository: WorkspaceRepository,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }
    val hotseatApps = remember(apps, workspace?.hotseatPins) {
        HotseatResolver.resolveHotseatApps(
            apps = apps,
            pinnedKeys = workspace?.hotseatPins ?: emptyList(),
        )
    }
    val gridApps = remember(apps, hotseatApps) {
        apps.filter { app -> hotseatApps.none { it.componentName == app.componentName } }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        rootWidthPx = with(density) { maxWidth.toPx() }
        rootHeightPx = with(density) { maxHeight.toPx() }

        LauncherWorkspacePointerEffects(
            apps = apps,
            itemBounds = itemBounds,
            rootWidthPx = rootWidthPx,
            rootHeightPx = rootHeightPx,
            onLaunchApp = onLaunchApp,
            onToggleHotseatPin = onToggleHotseatPin,
        )

        GlassesSpatialWorkspaceScreen(
            apps = gridApps,
            hotseatApps = hotseatApps,
            pinnedComponentKeys = workspace?.hotseatPins?.toSet() ?: emptySet(),
            onBoundsChanged = { key, rect -> itemBounds[key] = rect },
        )
    }
}
