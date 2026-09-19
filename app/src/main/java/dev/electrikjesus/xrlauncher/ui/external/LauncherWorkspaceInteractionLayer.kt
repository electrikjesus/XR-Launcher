package dev.electrikjesus.xrlauncher.ui.external

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset
import dev.electrikjesus.xrlauncher.core.workspace.PanelBounds
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.workspace.LauncherContextMenuHost
import dev.electrikjesus.xrlauncher.ui.workspace.openAppContextMenuFromBounds
import kotlinx.coroutines.launch

@Composable
fun LauncherWorkspaceInteractionLayer(
    launchableApps: List<LaunchableApp>,
    panels: List<PanelState>,
    pinnedComponentKeys: Set<String>,
    itemBounds: Map<String, Rect>,
    panelBounds: Map<String, Rect>,
    rootWidthPx: Float,
    rootHeightPx: Float,
    workspaceRepository: WorkspaceRepository,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    onPanelBoundsChanged: (String, PanelBounds) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {},
    onOpenAllApps: () -> Unit = {},
    onLayoutPresetSelected: (LayoutPreset) -> Unit = {},
    onTuneAppearance: (HomeSpaceTuneAxis, Float) -> Unit = { _, _ -> },
    panelScale: Float = 1f,
    sphereScale: Float = 1f,
    showContextMenu: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    LauncherWorkspacePointerEffects(
        apps = launchableApps,
        panels = panels,
        pinnedComponentKeys = pinnedComponentKeys,
        itemBounds = itemBounds,
        panelBounds = panelBounds,
        rootWidthPx = rootWidthPx,
        rootHeightPx = rootHeightPx,
        onLaunchApp = onLaunchApp,
        onOpenSettings = onOpenSettings,
        onOpenAllApps = onOpenAllApps,
        onLayoutPresetSelected = onLayoutPresetSelected,
        onTuneAppearance = onTuneAppearance,
        panelScale = panelScale,
        sphereScale = sphereScale,
    )

    WorkspacePanelFocusEffects(
        panelIds = panels.filter { it.visible }.map { it.id },
        panelBounds = panelBounds,
        rootWidthPx = rootWidthPx,
        rootHeightPx = rootHeightPx,
    )

    PanelHandlePointerEffects(
        panels = panels,
        panelBounds = panelBounds,
        rootWidthPx = rootWidthPx,
        rootHeightPx = rootHeightPx,
        onPanelBoundsChanged = onPanelBoundsChanged,
    )

    PanelChromePointerEffects(
        panels = panels,
        panelBounds = panelBounds,
        rootWidthPx = rootWidthPx,
        rootHeightPx = rootHeightPx,
        onMinimizePanel = { panelId ->
            scope.launch { workspaceRepository.setPanelMinimized(panelId, minimized = true) }
        },
        onClosePanel = { panelId ->
            scope.launch { workspaceRepository.setPanelVisible(panelId, visible = false) }
        },
        onRestorePanel = { panelId ->
            scope.launch { workspaceRepository.setPanelMinimized(panelId, minimized = false) }
        },
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (showContextMenu) {
            LauncherContextMenuHost(
            rootWidthPx = rootWidthPx,
            rootHeightPx = rootHeightPx,
            onLaunchApp = onLaunchApp,
            onToggleHotseatPin = onToggleHotseatPin,
            onHidePanel = { panelId ->
                scope.launch { workspaceRepository.setPanelVisible(panelId, visible = false) }
            },
            onSnapPanelToGrid = { panelId ->
                scope.launch { workspaceRepository.snapPanelToDefaultGrid(panelId) }
            },
            )
        }
    }
}

/** @return handler to pass into app grids for long-press context menus. */
fun appContextMenuHandler(
    pinnedComponentKeys: Set<String>,
    rootWidthPx: Float,
    rootHeightPx: Float,
): (LaunchableApp, Rect) -> Unit = { app, bounds ->
    openAppContextMenuFromBounds(
        app = app,
        bounds = bounds,
        isPinned = app.componentKey() in pinnedComponentKeys,
        rootWidthPx = rootWidthPx,
        rootHeightPx = rootHeightPx,
    )
}
