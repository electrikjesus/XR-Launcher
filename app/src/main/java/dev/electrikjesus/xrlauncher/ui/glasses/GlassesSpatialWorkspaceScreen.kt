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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.launcher.AppDrawerItem
import dev.electrikjesus.xrlauncher.core.launcher.AppDrawerLayout
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.external.ExternalCursorDot
import dev.electrikjesus.xrlauncher.ui.workspace.AppIconCell
import dev.electrikjesus.xrlauncher.ui.workspace.EmptySlotPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WidgetPanelById
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceWallpaper

@Composable
fun GlassesSpatialWorkspaceScreen(
    apps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String> = emptySet(),
    panels: List<PanelState> = Workspace.defaultPanels(),
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val desktopOverlayActive by DisplayPointerInjector.isAvailableFlow.collectAsState()
    val showInAppCursor = !desktopOverlayActive
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        AppRepository.filterLaunchableApps(apps, searchQuery)
    }
    val drawerItems = remember(filteredApps, searchQuery) {
        AppDrawerLayout.buildItems(filteredApps, searchQuery)
    }
    val visiblePanels = remember(panels) { panels.filter { it.visible } }

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

            if (cursor.hoveredLabel != null) {
                Text(
                    text = stringResource(R.string.cursor_over, cursor.hoveredLabel!!),
                    color = Color(0xFF03DAC5),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            GlassesPanelLayout(
                panels = visiblePanels,
                hotseatApps = hotseatApps,
                pinnedComponentKeys = pinnedComponentKeys,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                drawerItems = drawerItems,
                hoveredLabel = cursor.hoveredLabel,
                onBoundsChanged = onBoundsChanged,
                onLaunchApp = onLaunchApp,
                modifier = Modifier.weight(1f),
            )
        }

        if (showInAppCursor) {
            ExternalCursorDot(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GlassesPanelLayout(
    panels: List<PanelState>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerItems: List<AppDrawerItem>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        var index = 0
        while (index < panels.size) {
            val panel = panels[index]
            when (panel.kind) {
                PanelKind.WIDGET -> {
                    val widgetPanels = panels.drop(index).takeWhile { it.kind == PanelKind.WIDGET }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        widgetPanels.forEach { widgetPanel ->
                            WidgetPanelById(
                                widgetId = widgetPanel.id,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    index += widgetPanels.size
                }
                PanelKind.APP_DRAWER -> {
                    AppDrawerPanel(
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        drawerItems = drawerItems,
                        hoveredLabel = hoveredLabel,
                        pinnedComponentKeys = pinnedComponentKeys,
                        onBoundsChanged = onBoundsChanged,
                        onLaunchApp = onLaunchApp,
                        modifier = Modifier.weight(1f),
                    )
                    index++
                }
                PanelKind.HOTSEAT -> {
                    HotseatRow(
                        apps = hotseatApps,
                        hoveredLabel = hoveredLabel,
                        pinnedComponentKeys = pinnedComponentKeys,
                        onBoundsChanged = onBoundsChanged,
                        onLaunchApp = onLaunchApp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    index++
                }
                PanelKind.EMPTY_SLOT -> {
                    EmptySlotPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    index++
                }
            }
        }
    }
}

@Composable
private fun AppDrawerPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerItems: List<AppDrawerItem>,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.workspace_panel_drawer),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
            },
            placeholder = {
                Text(
                    stringResource(R.string.search_apps),
                    color = Color.White.copy(alpha = 0.5f),
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                cursorColor = Color(0xFF03DAC5),
                focusedBorderColor = Color(0xFF03DAC5),
                unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
            ),
        )
        if (drawerItems.isEmpty()) {
            Text(
                text = stringResource(R.string.no_apps_found),
                color = Color.White.copy(alpha = 0.6f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(
                    items = drawerItems,
                    key = { item ->
                        when (item) {
                            is AppDrawerItem.SectionHeader -> "header-${item.letter}"
                            is AppDrawerItem.AppEntry -> item.app.componentName.flattenToString()
                        }
                    },
                    span = { item ->
                        when (item) {
                            is AppDrawerItem.SectionHeader -> GridItemSpan(maxLineSpan)
                            is AppDrawerItem.AppEntry -> GridItemSpan(1)
                        }
                    },
                ) { item ->
                    when (item) {
                        is AppDrawerItem.SectionHeader -> {
                            Text(
                                text = item.letter.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        is AppDrawerItem.AppEntry -> {
                            AppIconCell(
                                app = item.app,
                                isHovered = hoveredLabel == item.app.label,
                                isPinned = item.app.componentKey() in pinnedComponentKeys,
                                onBoundsChanged = onBoundsChanged,
                                onLaunchApp = onLaunchApp,
                            )
                        }
                    }
                }
            }
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
