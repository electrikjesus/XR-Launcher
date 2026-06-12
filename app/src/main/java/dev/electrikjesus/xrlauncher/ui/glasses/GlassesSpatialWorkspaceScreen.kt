package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.launcher.AppDrawerItem
import dev.electrikjesus.xrlauncher.core.launcher.AppDrawerLayout
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import androidx.compose.foundation.layout.BoxWithConstraints
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.PanelBounds
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLayoutPresets
import dev.electrikjesus.xrlauncher.ui.workspace.DraggableWorkspacePanelShell
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.ui.external.ExternalCursorDot
import dev.electrikjesus.xrlauncher.ui.workspace.EmptySlotPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WidgetPanelById
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceAppDrawerPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceHotseatRow
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspacePanelShell
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceWallpaper

@Composable
fun GlassesSpatialWorkspaceScreen(
    apps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String> = emptySet(),
    panels: List<PanelState> = Workspace.defaultPanels(),
    onBoundsChanged: (String, Rect) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit = { _, _ -> },
    onPanelsChange: (List<PanelState>) -> Unit = {},
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
    val focusedPanelId by CompanionPointerBus.focusedPanelId.collectAsState()
    val launcherForeground by GlassesSessionState.launcherForegroundFlow.collectAsState()
    val useFreeform = remember(panels) { WorkspaceLayoutPresets.usesFreeformLayout(panels) }
    val inferredPreset = remember(panels) { WorkspaceLayoutPresets.inferPreset(panels) }
    var activePreset by remember(panels) { mutableStateOf(inferredPreset) }

    LaunchedEffect(panels) {
        activePreset = inferredPreset
    }

    LaunchedEffect(panels, visiblePanels) {
        if (CompanionPointerBus.focusedPanelId.value == null && visiblePanels.isNotEmpty()) {
            CompanionPointerBus.setFocusedPanelId(visiblePanels.first().id)
        }
    }

    fun updatePanelBounds(panelId: String, bounds: PanelBounds) {
        onPanelsChange(
            panels.map { panel ->
                if (panel.id == panelId) panel.copy(bounds = bounds.clamp()) else panel
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        val parallaxX = (cursor.x - 0.5f) * 2f
        val parallaxY = (cursor.y - 0.5f) * 2f
        WorkspaceWallpaper(
            parallaxX = parallaxX,
            parallaxY = parallaxY,
            modifier = Modifier.fillMaxSize(),
        )

        if (useFreeform) {
            FreeformGlassesPanelLayout(
                panels = visiblePanels,
                focusedPanelId = focusedPanelId,
                hotseatApps = hotseatApps,
                pinnedComponentKeys = pinnedComponentKeys,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                drawerItems = drawerItems,
                hoveredLabel = cursor.hoveredLabel,
                onBoundsChanged = onBoundsChanged,
                onPanelBoundsChanged = onPanelBoundsChanged,
                onPanelFrameChanged = ::updatePanelBounds,
                onLaunchApp = onLaunchApp,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = parallaxX * -14f
                        translationY = parallaxY * -10f
                    },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .graphicsLayer {
                        translationX = parallaxX * -10f
                        translationY = parallaxY * -8f
                    },
            ) {
                GlassesPanelLayout(
                    panels = visiblePanels,
                    focusedPanelId = focusedPanelId,
                    hotseatApps = hotseatApps,
                    pinnedComponentKeys = pinnedComponentKeys,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    drawerItems = drawerItems,
                    hoveredLabel = cursor.hoveredLabel,
                    onBoundsChanged = onBoundsChanged,
                    onPanelBoundsChanged = onPanelBoundsChanged,
                    onLaunchApp = onLaunchApp,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Floating HUD — does not cover the environment backdrop.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .graphicsLayer {
                    translationX = parallaxX * -6f
                    translationY = parallaxY * -4f
                },
        ) {
            WorkspaceLayoutPresetBar(
                activePreset = activePreset,
                onPresetSelected = { preset ->
                    activePreset = preset
                    onPanelsChange(WorkspaceLayoutPresets.apply(panels, preset))
                },
            )
            if (cursor.hoveredLabel != null) {
                Text(
                    text = stringResource(R.string.cursor_over, cursor.hoveredLabel!!),
                    color = Color(0xFF03DAC5),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (focusedPanelId != null && launcherForeground) {
                Text(
                    text = stringResource(
                        R.string.workspace_focused_panel,
                        panelTitleLabel(focusedPanelId!!),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (showInAppCursor) {
            ExternalCursorDot(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GlassesPanelLayout(
    panels: List<PanelState>,
    focusedPanelId: String?,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerItems: List<AppDrawerItem>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit,
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
                            WorkspacePanelShell(
                                panelId = widgetPanel.id,
                                title = panelTitle(widgetPanel),
                                isFocused = focusedPanelId == widgetPanel.id,
                                onPanelBoundsChanged = onPanelBoundsChanged,
                                modifier = Modifier.weight(1f),
                            ) {
                                WidgetPanelById(widgetId = widgetPanel.id)
                            }
                        }
                    }
                    index += widgetPanels.size
                }
                PanelKind.APP_DRAWER -> {
                    WorkspacePanelShell(
                        panelId = panel.id,
                        title = panelTitle(panel),
                        isFocused = focusedPanelId == panel.id,
                        onPanelBoundsChanged = onPanelBoundsChanged,
                        modifier = Modifier.weight(1f),
                    ) {
                        WorkspaceAppDrawerPanel(
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            drawerItems = drawerItems,
                            hoveredLabel = hoveredLabel,
                            pinnedComponentKeys = pinnedComponentKeys,
                            onBoundsChanged = onBoundsChanged,
                            onLaunchApp = onLaunchApp,
                        )
                    }
                    index++
                }
                PanelKind.HOTSEAT -> {
                    WorkspacePanelShell(
                        panelId = panel.id,
                        title = panelTitle(panel),
                        isFocused = focusedPanelId == panel.id,
                        onPanelBoundsChanged = onPanelBoundsChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        WorkspaceHotseatRow(
                            apps = hotseatApps,
                            hoveredLabel = hoveredLabel,
                            pinnedComponentKeys = pinnedComponentKeys,
                            onBoundsChanged = onBoundsChanged,
                            onLaunchApp = onLaunchApp,
                        )
                    }
                    index++
                }
                PanelKind.EMPTY_SLOT -> {
                    WorkspacePanelShell(
                        panelId = panel.id,
                        title = panelTitle(panel),
                        isFocused = focusedPanelId == panel.id,
                        onPanelBoundsChanged = onPanelBoundsChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        EmptySlotPanel()
                    }
                    index++
                }
            }
        }
    }
}

@Composable
private fun FreeformGlassesPanelLayout(
    panels: List<PanelState>,
    focusedPanelId: String?,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerItems: List<AppDrawerItem>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    onPanelFrameChanged: (String, PanelBounds) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        panels.filter { it.bounds != null }.forEach { panel ->
            DraggableWorkspacePanelShell(
                panel = panel,
                title = panelTitle(panel),
                isFocused = focusedPanelId == panel.id,
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                onBoundsChanged = { bounds -> onPanelFrameChanged(panel.id, bounds) },
                onPanelBoundsChanged = onPanelBoundsChanged,
            ) {
                PanelBody(
                    panel = panel,
                    hotseatApps = hotseatApps,
                    pinnedComponentKeys = pinnedComponentKeys,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    drawerItems = drawerItems,
                    hoveredLabel = hoveredLabel,
                    onBoundsChanged = onBoundsChanged,
                    onLaunchApp = onLaunchApp,
                )
            }
        }
    }
}

@Composable
private fun PanelBody(
    panel: PanelState,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerItems: List<AppDrawerItem>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
) {
    when (panel.kind) {
        PanelKind.WIDGET -> WidgetPanelById(widgetId = panel.id)
        PanelKind.APP_DRAWER -> WorkspaceAppDrawerPanel(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            drawerItems = drawerItems,
            hoveredLabel = hoveredLabel,
            pinnedComponentKeys = pinnedComponentKeys,
            onBoundsChanged = onBoundsChanged,
            onLaunchApp = onLaunchApp,
        )
        PanelKind.HOTSEAT -> WorkspaceHotseatRow(
            apps = hotseatApps,
            hoveredLabel = hoveredLabel,
            pinnedComponentKeys = pinnedComponentKeys,
            onBoundsChanged = onBoundsChanged,
            onLaunchApp = onLaunchApp,
        )
        PanelKind.EMPTY_SLOT -> EmptySlotPanel()
    }
}

@Composable
private fun panelTitleLabel(panelId: String): String = when (panelId) {
    "widget_clock" -> stringResource(R.string.workspace_panel_clock)
    "widget_calendar" -> stringResource(R.string.workspace_panel_calendar)
    "app_drawer" -> stringResource(R.string.workspace_panel_drawer)
    "hotseat" -> stringResource(R.string.workspace_panel_hotseat)
    "empty_slot" -> stringResource(R.string.workspace_empty_slot)
    else -> panelId
}

@Composable
private fun panelTitle(panel: PanelState): String = panelTitleLabel(panel.id)
