package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.external.ExternalCursorDot
import dev.electrikjesus.xrlauncher.ui.workspace.AppIconCell
import dev.electrikjesus.xrlauncher.ui.workspace.ClockWidgetPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceWallpaper

@Composable
fun GlassesSpatialWorkspaceScreen(
    apps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String> = emptySet(),
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val desktopOverlayActive by DisplayPointerInjector.isAvailableFlow.collectAsState()
    val showInAppCursor = !desktopOverlayActive

    Box(modifier = modifier.fillMaxSize()) {
        val parallaxX = (cursor.x - 0.5f) * 2f
        val parallaxY = (cursor.y - 0.5f) * 2f
        WorkspaceWallpaper(parallaxX = parallaxX, parallaxY = parallaxY)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    translationX = parallaxX * -8f
                    translationY = parallaxY * -6f
                },
        ) {
            Text(
                text = stringResource(R.string.glasses_workspace),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.85f),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ClockWidgetPanel(modifier = Modifier.weight(1f))
            }

            if (cursor.hoveredLabel != null) {
                Text(
                    text = stringResource(R.string.cursor_over, cursor.hoveredLabel!!),
                    color = Color(0xFF03DAC5),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(apps, key = { it.componentName.flattenToString() }) { app ->
                    AppIconCell(
                        app = app,
                        isHovered = cursor.hoveredLabel == app.label,
                        isPinned = app.componentKey() in pinnedComponentKeys,
                        onBoundsChanged = onBoundsChanged,
                        onLaunchApp = onLaunchApp,
                    )
                }
            }

            HotseatRow(
                apps = hotseatApps,
                hoveredLabel = cursor.hoveredLabel,
                pinnedComponentKeys = pinnedComponentKeys,
                onBoundsChanged = onBoundsChanged,
                onLaunchApp = onLaunchApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        if (showInAppCursor) {
            ExternalCursorDot(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun HotseatRow(
    apps: List<LaunchableApp>,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        apps.forEach { app ->
            AppIconCell(
                app = app,
                isHovered = hoveredLabel == app.label,
                isPinned = app.componentKey() in pinnedComponentKeys,
                onBoundsChanged = onBoundsChanged,
                onLaunchApp = onLaunchApp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
