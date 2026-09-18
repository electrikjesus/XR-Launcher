package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsOverlayHits
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@Composable
fun WorkspaceAllAppsOverlay(
    appCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    apps: List<LaunchableApp>,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onDismiss: () -> Unit,
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.94f)
                .fillMaxSize(0.88f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121212).copy(alpha = 0.96f),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text(
                            text = stringResource(R.string.all_apps),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                        if (appCount > 0) {
                            Text(
                                text = stringResource(R.string.all_apps_count, appCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(56.dp)
                            .onGloballyPositioned { coordinates ->
                                onBoundsChanged(
                                    AllAppsOverlayHits.CLOSE_BOUNDS_KEY,
                                    coordinates.boundsInRoot(),
                                )
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.all_apps_close),
                            tint = if (hoveredLabel == AllAppsOverlayHits.CLOSE_HOVER_LABEL) {
                                Color(0xFF03DAC5)
                            } else {
                                Color.White.copy(alpha = 0.85f)
                            },
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                WorkspaceAppDrawerPanel(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    apps = apps,
                    hoveredLabel = hoveredLabel,
                    pinnedComponentKeys = pinnedComponentKeys,
                    onBoundsChanged = onBoundsChanged,
                    onLaunchApp = { app ->
                        onLaunchApp(app)
                        onDismiss()
                    },
                    onAppContextMenu = onAppContextMenu,
                    useSharedPagination = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                )
            }
        }
    }
}
