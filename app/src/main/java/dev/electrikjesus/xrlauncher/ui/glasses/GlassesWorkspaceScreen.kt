package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun GlassesWorkspaceScreen(
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val workspaceRepository = remember { WorkspaceRepository(context.applicationContext) }
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val hotseatApps = remember(apps, workspace?.hotseatPins) {
        HotseatResolver.resolveHotseatApps(
            apps = apps,
            pinnedKeys = workspace?.hotseatPins ?: emptyList(),
        )
    }
    val gridApps = remember(apps, hotseatApps) {
        apps.filter { app -> hotseatApps.none { it.componentName == app.componentName } }
    }

    Surface(
        modifier = modifier
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
                    onBoundsChanged = { _, _ -> },
                    onLaunchApp = onLaunchApp,
                )
            }
        }
    }
}
