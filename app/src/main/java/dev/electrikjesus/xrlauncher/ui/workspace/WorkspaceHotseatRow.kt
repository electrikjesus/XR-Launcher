package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.componentKey

@Composable
fun WorkspaceHotseatRow(
    apps: List<LaunchableApp>,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    onOpenAllApps: (() -> Unit)? = null,
    allAppsHovered: Boolean = false,
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
        if (onOpenAllApps != null) {
            AllAppsLauncherCell(
                isHovered = allAppsHovered,
                onBoundsChanged = onBoundsChanged,
                onOpenAllApps = onOpenAllApps,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
