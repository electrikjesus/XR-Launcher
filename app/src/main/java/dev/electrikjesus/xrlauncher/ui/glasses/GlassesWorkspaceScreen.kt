package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.ui.shared.AppGridPanel

@Composable
fun GlassesWorkspaceScreen(
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                AppGridPanel(
                    title = stringResource(R.string.glasses_workspace),
                    apps = apps,
                    onLaunchApp = onLaunchApp,
                    onFocus = {},
                )
            }
        }
    }
}
