package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.capability.SpatialEmbedCapability
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.launcher.AppSystemActions
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.workspace.DeskArrangeMode
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconTextureBus
import dev.electrikjesus.xrlauncher.core.workspace.DeskLassoState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDeskState
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuTarget
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.RadialMenuGeometry
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlin.math.roundToInt

fun openAppContextMenuFromBounds(
    app: LaunchableApp,
    bounds: Rect,
    isPinned: Boolean,
    rootWidthPx: Float,
    rootHeightPx: Float,
) {
    if (rootWidthPx <= 0f || rootHeightPx <= 0f) return
    LauncherContextMenuState.openApp(
        app = app,
        isPinned = isPinned,
        anchorX = bounds.center.x / rootWidthPx,
        anchorY = bounds.center.y / rootHeightPx,
    )
}

@Composable
fun LauncherContextMenuHost(
    rootWidthPx: Float,
    rootHeightPx: Float,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    onHidePanel: (String) -> Unit,
    onSnapPanelToGrid: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val request by LauncherContextMenuState.request.collectAsState()
    if (request == null || rootWidthPx <= 0f || rootHeightPx <= 0f) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val selectedKeys by DeskLassoState.selectedKeysFlow.collectAsState()
    val anchorXPx = request!!.anchorX * rootWidthPx
    val anchorYPx = request!!.anchorY * rootHeightPx
    val radiusPx = with(density) { 128.dp.toPx() }
    val chipWidthPx = with(density) { 168.dp.toPx() }
    val chipHeightPx = with(density) { 44.dp.toPx() }

    val actions = when (val target = request!!.target) {
        is LauncherContextMenuTarget.App -> appRadialActions(
            app = target.app,
            isPinned = target.isPinned,
            showClear = target.app.componentKey() in selectedKeys && selectedKeys.size > 1,
            onLaunchApp = onLaunchApp,
            onToggleHotseatPin = onToggleHotseatPin,
            context = context,
        )
        is LauncherContextMenuTarget.Panel -> panelRadialActions(
            panelId = target.panelId,
            onHidePanel = onHidePanel,
            onSnapPanelToGrid = onSnapPanelToGrid,
        )
        is LauncherContextMenuTarget.Desktop -> desktopRadialActions(
            showClear = selectedKeys.isNotEmpty(),
        )
    }
    val title = when (val target = request!!.target) {
        is LauncherContextMenuTarget.App -> target.app.label
        is LauncherContextMenuTarget.Panel -> panelTitle(target.panelId, target.kind)
        is LauncherContextMenuTarget.Desktop -> if (selectedKeys.isEmpty()) {
            stringResource(R.string.xr_desktop)
        } else {
            stringResource(R.string.context_menu_selection_count, selectedKeys.size)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { LauncherContextMenuState.dismiss() },
            ),
    ) {
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (anchorXPx - chipWidthPx / 2f)
                            .coerceIn(8f, rootWidthPx - chipWidthPx - 8f)
                            .roundToInt(),
                        (anchorYPx - chipHeightPx / 2f)
                            .coerceIn(8f, rootHeightPx - chipHeightPx - 8f)
                            .roundToInt(),
                    )
                }
                .zIndex(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        actions.forEachIndexed { index, action ->
            val (dx, dy) = RadialMenuGeometry.slotOffsetPx(index, actions.size, radiusPx)
            val x = (anchorXPx + dx - chipWidthPx / 2f)
                .coerceIn(8f, (rootWidthPx - chipWidthPx - 8f).coerceAtLeast(8f))
            val y = (anchorYPx + dy - chipHeightPx / 2f)
                .coerceIn(8f, (rootHeightPx - chipHeightPx - 8f).coerceAtLeast(8f))
            Surface(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .zIndex(2f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = action.onClick,
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
            ) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private data class RadialAction(val label: String, val onClick: () -> Unit)

@Composable
private fun appRadialActions(
    app: LaunchableApp,
    isPinned: Boolean,
    showClear: Boolean,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    context: android.content.Context,
): List<RadialAction> {
    val launchPanel = launchPanelForContextMenu()
    val launchMode = SpatialEmbedCapability.launchLabel(context, launchPanel)
    val dismiss = { LauncherContextMenuState.dismiss() }
    val actions = mutableListOf(
        RadialAction(stringResource(R.string.context_menu_open_app_with_mode, launchMode)) {
            dismiss()
            DeskLassoState.clearSelection()
            onLaunchApp(app)
        },
        RadialAction(
            if (isPinned) {
                stringResource(R.string.context_menu_remove_from_hotseat)
            } else {
                stringResource(R.string.context_menu_add_to_hotseat)
            },
        ) {
            dismiss()
            onToggleHotseatPin(app)
        },
        RadialAction(stringResource(R.string.context_menu_app_info)) {
            dismiss()
            AppSystemActions.openAppInfo(context, app)
        },
        RadialAction(stringResource(R.string.context_menu_uninstall)) {
            dismiss()
            AppSystemActions.requestUninstall(context, app)
        },
    )
    if (showClear) {
        actions += RadialAction(stringResource(R.string.context_menu_clear_selection)) {
            dismiss()
            DeskLassoState.clearSelection()
        }
    }
    return actions
}

@Composable
private fun panelRadialActions(
    panelId: String,
    onHidePanel: (String) -> Unit,
    onSnapPanelToGrid: (String) -> Unit,
): List<RadialAction> {
    val dismiss = { LauncherContextMenuState.dismiss() }
    return listOf(
        RadialAction(stringResource(R.string.context_menu_focus_panel)) {
            dismiss()
            CompanionPointerBus.setFocusedPanelId(panelId)
        },
        RadialAction(stringResource(R.string.context_menu_snap_to_grid)) {
            dismiss()
            onSnapPanelToGrid(panelId)
        },
        RadialAction(stringResource(R.string.context_menu_hide_panel)) {
            dismiss()
            onHidePanel(panelId)
        },
    )
}

@Composable
private fun desktopRadialActions(showClear: Boolean): List<RadialAction> {
    val dismiss = { LauncherContextMenuState.dismiss() }
    val selected = DeskLassoState.selectedKeys
    if (selected.isNotEmpty()) {
        val halfW = DeskIconTextureBus.icons()
            .firstOrNull { it.componentKey in selected }
            ?.halfWidth
            ?: HomeSpaceDesk.ICON_HALF_WIDTH
        val halfH = DeskIconTextureBus.icons()
            .firstOrNull { it.componentKey in selected }
            ?.halfHeight
            ?: HomeSpaceDesk.labeledIconHalfHeight(halfW)
        fun arrange(mode: DeskArrangeMode) {
            dismiss()
            HomeSpaceDeskState.arrangeSelected(
                keys = selected,
                mode = mode,
                sphereScale = 1f,
                halfWidth = halfW,
                halfHeight = halfH,
            )
            DeskLassoState.clearSelection()
        }
        return buildList {
            if (selected.size >= 2) {
                add(
                    RadialAction(stringResource(R.string.context_menu_arrange_stack)) {
                        arrange(DeskArrangeMode.STACK)
                    },
                )
                add(
                    RadialAction(stringResource(R.string.context_menu_arrange_folder)) {
                        arrange(DeskArrangeMode.FOLDER)
                    },
                )
                add(
                    RadialAction(stringResource(R.string.context_menu_arrange_row)) {
                        arrange(DeskArrangeMode.ROW)
                    },
                )
                add(
                    RadialAction(stringResource(R.string.context_menu_arrange_column)) {
                        arrange(DeskArrangeMode.COLUMN)
                    },
                )
                add(
                    RadialAction(stringResource(R.string.context_menu_arrange_grid)) {
                        arrange(DeskArrangeMode.GRID)
                    },
                )
            }
            add(
                RadialAction(stringResource(R.string.context_menu_clear_selection)) {
                    dismiss()
                    DeskLassoState.clearSelection()
                },
            )
            add(
                RadialAction(stringResource(R.string.context_menu_remove_from_desktop)) {
                    dismiss()
                    HomeSpaceDeskState.removeByKeys(selected)
                    DeskLassoState.clearSelection()
                },
            )
        }
    }
    val actions = mutableListOf(
        RadialAction(stringResource(R.string.context_menu_open_all_apps)) {
            dismiss()
            GlassesSessionState.showAllAppsOverlay()
        },
    )
    if (showClear) {
        actions += RadialAction(stringResource(R.string.context_menu_clear_selection)) {
            dismiss()
            DeskLassoState.clearSelection()
        }
    }
    return actions
}

@Composable
private fun panelTitle(panelId: String, kind: PanelKind): String = when (panelId) {
    "widget_clock" -> stringResource(R.string.workspace_panel_clock)
    "widget_calendar" -> stringResource(R.string.workspace_panel_calendar)
    "app_drawer" -> stringResource(R.string.workspace_panel_drawer)
    "hotseat" -> stringResource(R.string.workspace_panel_hotseat)
    "empty_slot" -> stringResource(R.string.workspace_empty_slot)
        else -> when (kind) {
        PanelKind.WIDGET -> stringResource(R.string.context_menu_widget, panelId)
        else -> panelId
    }
}

private fun launchPanelForContextMenu(): PanelState {
    val focusedId = CompanionPointerBus.focusedPanelId.value
    return when (focusedId) {
        "empty_slot" -> PanelState(id = "empty_slot", kind = PanelKind.EMPTY_SLOT, visible = true)
        else -> PanelState(id = "full_window", kind = PanelKind.EMPTY_SLOT)
    }
}
