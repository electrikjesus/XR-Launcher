package dev.electrikjesus.xrlauncher.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.launcher.AppDrawerLayout
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.Workspace
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLayoutPresets
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.external.rememberDebouncedPanelSaver
import dev.electrikjesus.xrlauncher.ui.workspace.EmptySlotPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WidgetPanelById
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceAppDrawerPanel
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceHotseatRow
import kotlinx.coroutines.launch

@Composable
fun SpatialDesktopScreen(
    apps: List<LaunchableApp>,
    workspaceRepository: WorkspaceRepository,
    onLaunchApp: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val camera by CompanionPointerBus.camera.collectAsState()
    val focusedPanelIndex by CompanionPointerBus.focusedPanelIndex.collectAsState()
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    val scope = rememberCoroutineScope()
    val panelSaver = rememberDebouncedPanelSaver(workspaceRepository)

    val panels = workspace?.panels ?: Workspace.defaultPanels()
    val visiblePanels = remember(panels) {
        panels.filter { it.visible && it.kind != PanelKind.EMPTY_SLOT }
    }
    val hotseatApps = remember(apps, workspace?.hotseatPins) {
        HotseatResolver.resolveHotseatApps(
            apps = apps,
            pinnedKeys = workspace?.hotseatPins ?: emptyList(),
        )
    }
    val gridApps = remember(apps, hotseatApps) {
        apps.filter { app -> hotseatApps.none { it.componentName == app.componentName } }
    }
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(gridApps, searchQuery) {
        AppRepository.filterLaunchableApps(gridApps, searchQuery)
    }
    val drawerItems = remember(filteredApps, searchQuery) {
        AppDrawerLayout.buildItems(filteredApps, searchQuery)
    }
    val pinnedKeys = workspace?.hotseatPins?.toSet() ?: emptySet()

    LaunchedEffect(workspace?.focusedPanelIndex, visiblePanels.size) {
        val saved = workspace?.focusedPanelIndex ?: 0
        val clamped = saved.coerceIn(0, (visiblePanels.size - 1).coerceAtLeast(0))
        CompanionPointerBus.setFocusedPanel(clamped)
    }

    fun applyPreset(preset: LayoutPreset) {
        panelSaver.save(WorkspaceLayoutPresets.apply(panels, preset))
    }

    DesktopKeyboardLayer(
        panelCount = visiblePanels.size,
        visiblePanels = visiblePanels,
        gridApps = filteredApps,
        hotseatApps = hotseatApps,
        onLaunchApp = onLaunchApp,
        onApplyPreset = ::applyPreset,
        onFocusedIndexChanged = { index ->
            scope.launch { workspaceRepository.updateFocusedPanelIndex(index) }
        },
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        CompanionPointerBus.orbitCamera(
                            deltaYaw = dragAmount.x * 0.08f,
                            deltaPitch = -dragAmount.y * 0.08f,
                        )
                    }
                }
                .graphicsLayer {
                    rotationY = camera.yawDegrees
                    rotationX = camera.pitchDegrees
                },
        ) {
            Subspace {
                SpatialCurvedRow(curveRadius = 825.dp) {
                    visiblePanels.forEachIndexed { index, panel ->
                        SpatialDesktopPanel(
                            panel = panel,
                            index = index,
                            isFocused = focusedPanelIndex == index,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            drawerItems = drawerItems,
                            hotseatApps = hotseatApps,
                            pinnedComponentKeys = pinnedKeys,
                            hoveredLabel = cursor.hoveredLabel,
                            onBoundsChanged = { key, rect -> itemBounds[key] = rect },
                            onLaunchApp = onLaunchApp,
                            onFocus = {
                                CompanionPointerBus.setFocusedPanel(index)
                                scope.launch { workspaceRepository.updateFocusedPanelIndex(index) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpatialDesktopPanel(
    panel: PanelState,
    index: Int,
    isFocused: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerItems: List<dev.electrikjesus.xrlauncher.core.launcher.AppDrawerItem>,
    hotseatApps: List<LaunchableApp>,
    pinnedComponentKeys: Set<String>,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onFocus: () -> Unit,
) {
    val (width, height) = spatialPanelSize(panel.kind)
    val title = spatialPanelTitle(panel)

    SpatialPanel(
        modifier = SubspaceModifier
            .width(width)
            .height(height)
            .movable()
            .resizable(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onFocus),
            color = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                when (panel.kind) {
                    PanelKind.WIDGET -> WidgetPanelById(
                        widgetId = panel.id,
                        modifier = Modifier.weight(1f),
                    )
                    PanelKind.APP_DRAWER -> WorkspaceAppDrawerPanel(
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        drawerItems = drawerItems,
                        hoveredLabel = hoveredLabel,
                        pinnedComponentKeys = pinnedComponentKeys,
                        onBoundsChanged = onBoundsChanged,
                        onLaunchApp = onLaunchApp,
                        useDarkTheme = false,
                        modifier = Modifier.weight(1f),
                    )
                    PanelKind.HOTSEAT -> WorkspaceHotseatRow(
                        apps = hotseatApps,
                        hoveredLabel = hoveredLabel,
                        pinnedComponentKeys = pinnedComponentKeys,
                        onBoundsChanged = onBoundsChanged,
                        onLaunchApp = onLaunchApp,
                        modifier = Modifier.weight(1f),
                    )
                    PanelKind.EMPTY_SLOT -> EmptySlotPanel(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun spatialPanelTitle(panel: PanelState): String = when (panel.id) {
    "widget_clock" -> stringResource(R.string.workspace_panel_clock)
    "widget_calendar" -> stringResource(R.string.workspace_panel_calendar)
    "app_drawer" -> stringResource(R.string.workspace_panel_drawer)
    "hotseat" -> stringResource(R.string.workspace_panel_hotseat)
    else -> panel.id
}

private fun spatialPanelSize(kind: PanelKind): Pair<Dp, Dp> =
    when (kind) {
        PanelKind.WIDGET -> 320.dp to 220.dp
        PanelKind.APP_DRAWER -> 420.dp to 640.dp
        PanelKind.HOTSEAT -> 520.dp to 140.dp
        PanelKind.EMPTY_SLOT -> 360.dp to 240.dp
    }
