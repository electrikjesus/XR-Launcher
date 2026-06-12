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
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.external.LauncherWorkspacePointerEffects

/** Tier 2 / spatial-API path: movable `Subspace` panel wrapping the glasses launcher shell. */
@Composable
fun GlassesWorkspaceScreen(
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
            onToggleHotseatPin = onToggleHotseatPin,
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black,
        ) {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier
                        .width(960.dp)
                        .height(540.dp)
                        .movable()
                        .resizable(),
                ) {
                    GlassesSpatialWorkspaceScreen(
                        apps = gridApps,
                        hotseatApps = hotseatApps,
                        pinnedComponentKeys = workspace?.hotseatPins?.toSet() ?: emptySet(),
                        onBoundsChanged = { key, rect -> itemBounds[key] = rect },
                        onLaunchApp = onLaunchApp,
                    )
                }
            }
        }
    }
}
