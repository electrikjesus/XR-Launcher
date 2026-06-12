package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
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
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.ui.external.ExternalCursorDot
import dev.electrikjesus.xrlauncher.ui.workspace.EmptySlotPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WidgetPanelById
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.ui.workspace.AllAppsLauncher
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceAllAppsOverlay
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceAppDrawerPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceHotseatRow
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspacePanelShell
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceScaledLayer
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceWallpaper
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceWraparoundLayer
import dev.electrikjesus.xrlauncher.ui.workspace.WraparoundPanelContainer
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWraparound

@Composable
fun GlassesSpatialWorkspaceScreen(
    launchableApps: List<LaunchableApp>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String> = emptySet(),
    panels: List<PanelState> = Workspace.defaultPanels(),
    onBoundsChanged: (String, Rect) -> Unit,
    onPanelBoundsChanged: (String, Rect) -> Unit = { _, _ -> },
    onPanelsChange: (List<PanelState>) -> Unit = {},
    onLaunchApp: ((LaunchableApp) -> Unit)? = null,
    appearance: WorkspaceAppearance = WorkspaceAppearance.default(),
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val desktopOverlayActive by DisplayPointerInjector.isAvailableFlow.collectAsState()
    val showInAppCursor = !desktopOverlayActive
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(launchableApps, searchQuery) {
        AppRepository.filterLaunchableApps(launchableApps, searchQuery)
    }
    val drawerItems = remember(filteredApps, searchQuery) {
        AppDrawerLayout.buildItems(filteredApps, searchQuery)
    }
    var allAppsSearchQuery by remember { mutableStateOf("") }
    val filteredAllApps = remember(launchableApps, allAppsSearchQuery) {
        AppRepository.filterLaunchableApps(launchableApps, allAppsSearchQuery)
    }
    val allAppsDrawerItems = remember(filteredAllApps, allAppsSearchQuery) {
        AppDrawerLayout.buildItems(filteredAllApps, allAppsSearchQuery)
    }
    val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()

    LaunchedEffect(allAppsOverlayVisible) {
        if (allAppsOverlayVisible) {
            allAppsSearchQuery = ""
        }
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

    val tuned = appearance.clamped()
    val uiScale = tuned.uiScale
    val panelGapDp = tuned.panelGapDp
    val wrapCurvature = tuned.wrapCurvature
    val workspaceWidth = tuned.workspaceWidth
    val workspaceHeight = tuned.workspaceHeight
    val lookYaw = WorkspaceWraparound.effectiveLookYaw(tuned)
    val lookPitch = WorkspaceWraparound.effectiveLookPitch(tuned)
    val (parallaxX, parallaxY) = WorkspaceWraparound.cursorNorm(cursor.x, cursor.y)
    val (backdropYaw, backdropPitch) = WorkspaceWraparound.backdropLook(
        cursorX = cursor.x,
        cursorY = cursor.y,
        lookYawDegrees = lookYaw,
        lookPitchDegrees = lookPitch,
    )
    val openAllApps = { GlassesSessionState.showAllAppsOverlay() }
    val allAppsHovered = cursor.hoveredLabel == AllAppsLauncher.HOVER_LABEL

    Box(modifier = modifier.fillMaxSize()) {
        WorkspaceWallpaper(
            parallaxX = parallaxX,
            parallaxY = parallaxY,
            lookYawDegrees = backdropYaw,
            lookPitchDegrees = backdropPitch,
            modifier = Modifier.fillMaxSize(),
        )

        WorkspaceScaledLayer(uiScale = uiScale) {
            WorkspaceWraparoundLayer(
                appearance = tuned,
                cursorX = cursor.x,
                cursorY = cursor.y,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = if (useFreeform) 12.dp else 20.dp,
                                vertical = if (useFreeform) 8.dp else 16.dp,
                            ),
                    ) {
                        WorkspaceLayoutPresetBar(
                            activePreset = activePreset,
                            onPresetSelected = { preset ->
                                activePreset = preset
                                onPanelsChange(WorkspaceLayoutPresets.apply(panels, preset))
                            },
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
                                onOpenAllApps = openAllApps,
                                allAppsHovered = allAppsHovered,
                                panelGapDp = panelGapDp,
                                wrapCurvature = wrapCurvature,
                                workspaceWidth = workspaceWidth,
                                workspaceHeight = workspaceHeight,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
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
                                onOpenAllApps = openAllApps,
                                allAppsHovered = allAppsHovered,
                                panelGapDp = panelGapDp,
                                wrapCurvature = wrapCurvature,
                                workspaceWidth = workspaceWidth,
                                workspaceHeight = workspaceHeight,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    WorkspaceLauncherStatusHints(
                        hoveredLabel = cursor.hoveredLabel,
                        focusedPanelId = focusedPanelId,
                        launcherForeground = launcherForeground,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }

        if (showInAppCursor) {
            ExternalCursorDot(modifier = Modifier.fillMaxSize())
        }

        if (allAppsOverlayVisible && onLaunchApp != null) {
            WorkspaceAllAppsOverlay(
                appCount = launchableApps.size,
                searchQuery = allAppsSearchQuery,
                onSearchQueryChange = { allAppsSearchQuery = it },
                drawerItems = allAppsDrawerItems,
                hoveredLabel = cursor.hoveredLabel,
                pinnedComponentKeys = pinnedComponentKeys,
                onBoundsChanged = onBoundsChanged,
                onLaunchApp = onLaunchApp,
                onDismiss = { GlassesSessionState.hideAllAppsOverlay() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun WorkspaceLauncherStatusHints(
    hoveredLabel: String?,
    focusedPanelId: String?,
    launcherForeground: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hoveredLabel == null && (focusedPanelId == null || !launcherForeground)) return

    Column(modifier = modifier) {
        if (hoveredLabel != null) {
            val hoverText = if (hoveredLabel == AllAppsLauncher.HOVER_LABEL) {
                stringResource(R.string.all_apps)
            } else {
                hoveredLabel
            }
            Text(
                text = stringResource(R.string.cursor_over, hoverText),
                color = Color(0xFF03DAC5),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (focusedPanelId != null && launcherForeground) {
            Text(
                text = stringResource(
                    R.string.workspace_focused_panel,
                    panelTitleLabel(focusedPanelId),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
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
    onOpenAllApps: (() -> Unit)? = null,
    allAppsHovered: Boolean = false,
    panelGapDp: Float,
    wrapCurvature: Float,
    workspaceWidth: Float,
    workspaceHeight: Float,
    modifier: Modifier = Modifier,
) {
    val gap = panelGapDp.dp
    val rowSlots = remember(panels) { stackPanelCenters(panels) }
    var slotIndex = 0
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        var index = 0
        while (index < panels.size) {
            val panel = panels[index]
            when (panel.kind) {
                PanelKind.WIDGET -> {
                    val widgetPanels = panels.drop(index).takeWhile { it.kind == PanelKind.WIDGET }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        widgetPanels.forEach { widgetPanel ->
                            val slot = rowSlots[slotIndex++]
                            WraparoundPanelContainer(
                                panelId = widgetPanel.id,
                                centerXNorm = slot.centerX,
                                centerYNorm = slot.centerY,
                                wrapCurvature = wrapCurvature,
                                workspaceWidth = workspaceWidth,
                                workspaceHeight = workspaceHeight,
                                modifier = Modifier.weight(1f),
                            ) {
                                WorkspacePanelShell(
                                    panelId = widgetPanel.id,
                                    title = panelTitle(widgetPanel),
                                    isFocused = focusedPanelId == widgetPanel.id,
                                    onPanelBoundsChanged = onPanelBoundsChanged,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    WidgetPanelById(widgetId = widgetPanel.id)
                                }
                            }
                        }
                    }
                    index += widgetPanels.size
                }
                PanelKind.APP_DRAWER -> {
                    val slot = rowSlots[slotIndex++]
                    WraparoundPanelContainer(
                        panelId = panel.id,
                        centerXNorm = slot.centerX,
                        centerYNorm = slot.centerY,
                        wrapCurvature = wrapCurvature,
                        workspaceWidth = workspaceWidth,
                        workspaceHeight = workspaceHeight,
                        modifier = Modifier.weight(1f),
                    ) {
                        WorkspacePanelShell(
                            panelId = panel.id,
                            title = panelTitle(panel),
                            isFocused = focusedPanelId == panel.id,
                            onPanelBoundsChanged = onPanelBoundsChanged,
                            modifier = Modifier.fillMaxSize(),
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
                    }
                    index++
                }
                PanelKind.HOTSEAT -> {
                    val slot = rowSlots[slotIndex++]
                    WraparoundPanelContainer(
                        panelId = panel.id,
                        centerXNorm = slot.centerX,
                        centerYNorm = slot.centerY,
                        wrapCurvature = wrapCurvature,
                        workspaceWidth = workspaceWidth,
                        workspaceHeight = workspaceHeight,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        WorkspacePanelShell(
                            panelId = panel.id,
                            title = panelTitle(panel),
                            isFocused = focusedPanelId == panel.id,
                            onPanelBoundsChanged = onPanelBoundsChanged,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WorkspaceHotseatRow(
                                apps = hotseatApps,
                                hoveredLabel = hoveredLabel,
                                pinnedComponentKeys = pinnedComponentKeys,
                                onBoundsChanged = onBoundsChanged,
                                onLaunchApp = onLaunchApp,
                                onOpenAllApps = onOpenAllApps,
                                allAppsHovered = allAppsHovered,
                            )
                        }
                    }
                    index++
                }
                PanelKind.EMPTY_SLOT -> {
                    val slot = rowSlots[slotIndex++]
                    WraparoundPanelContainer(
                        panelId = panel.id,
                        centerXNorm = slot.centerX,
                        centerYNorm = slot.centerY,
                        wrapCurvature = wrapCurvature,
                        workspaceWidth = workspaceWidth,
                        workspaceHeight = workspaceHeight,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        WorkspacePanelShell(
                            panelId = panel.id,
                            title = panelTitle(panel),
                            isFocused = focusedPanelId == panel.id,
                            onPanelBoundsChanged = onPanelBoundsChanged,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            EmptySlotPanel()
                        }
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
    onOpenAllApps: (() -> Unit)? = null,
    allAppsHovered: Boolean = false,
    panelGapDp: Float,
    wrapCurvature: Float,
    workspaceWidth: Float,
    workspaceHeight: Float,
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
                panelGapDp = panelGapDp,
                wrapCurvature = wrapCurvature,
                workspaceWidth = workspaceWidth,
                workspaceHeight = workspaceHeight,
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
                    onOpenAllApps = onOpenAllApps,
                    allAppsHovered = allAppsHovered,
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
    onOpenAllApps: (() -> Unit)? = null,
    allAppsHovered: Boolean = false,
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
            onOpenAllApps = onOpenAllApps,
            allAppsHovered = allAppsHovered,
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

private data class StackPanelCenter(
    val centerX: Float,
    val centerY: Float,
)

/** Normalized panel centers for cylindrical wrap in stack layout. */
private fun stackPanelCenters(panels: List<PanelState>): List<StackPanelCenter> {
    val rows = mutableListOf<List<PanelState>>()
    var index = 0
    while (index < panels.size) {
        val panel = panels[index]
        if (panel.kind == PanelKind.WIDGET) {
            val widgets = panels.drop(index).takeWhile { it.kind == PanelKind.WIDGET }
            rows += listOf(widgets)
            index += widgets.size
        } else {
            rows += listOf(panel)
            index++
        }
    }
    val rowCount = rows.size.coerceAtLeast(1)
    return buildList {
        rows.forEachIndexed { rowIndex, rowPanels ->
            val centerY = (rowIndex + 0.5f) / rowCount
            if (rowPanels.size == 1) {
                add(StackPanelCenter(centerX = 0.5f, centerY = centerY))
            } else {
                rowPanels.forEachIndexed { colIndex, _ ->
                    add(
                        StackPanelCenter(
                            centerX = (colIndex + 0.5f) / rowPanels.size,
                            centerY = centerY,
                        ),
                    )
                }
            }
        }
    }
}
