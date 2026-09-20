package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.capability.SpatialEmbedCapability
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.launcher.AppSystemActions
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.DeskArrangeMode
import dev.electrikjesus.xrlauncher.core.workspace.DeskGroupMoveState
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconTextureBus
import dev.electrikjesus.xrlauncher.core.workspace.DeskLassoState
import dev.electrikjesus.xrlauncher.core.workspace.DeskPile
import dev.electrikjesus.xrlauncher.core.workspace.DeskPileMode
import dev.electrikjesus.xrlauncher.core.workspace.DeskWidgetController
import dev.electrikjesus.xrlauncher.core.workspace.DeskWidgetUtils
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDeskState
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuTarget
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.RadialMenuItem
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk

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

/**
 * Hosts BumpDesk [RadialMenuView] for right-click / long-press / lasso release.
 * Pie wedges + nested secondary ring (Create Pile / Layout), not Compose chips.
 */
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
    val selectedKeys by DeskLassoState.selectedKeysFlow.collectAsState()
    val context = LocalContext.current
    val shownRequest = remember { mutableStateOf<Any?>(null) }

    val items = remember(request, selectedKeys, onLaunchApp, onToggleHotseatPin, onHidePanel, onSnapPanelToGrid) {
        val req = request ?: return@remember emptyList()
        when (val target = req.target) {
            is LauncherContextMenuTarget.App -> appMenuItems(
                context = context,
                app = target.app,
                isPinned = target.isPinned,
                showClear = target.app.componentKey() in selectedKeys && selectedKeys.size > 1,
                onLaunchApp = onLaunchApp,
                onToggleHotseatPin = onToggleHotseatPin,
            )
            is LauncherContextMenuTarget.Panel -> panelMenuItems(
                context = context,
                panelId = target.panelId,
                onHidePanel = onHidePanel,
                onSnapPanelToGrid = onSnapPanelToGrid,
            )
            is LauncherContextMenuTarget.Desktop -> desktopMenuItems(
                context = context,
                selected = selectedKeys,
                deskYawDeg = req.deskYawDeg,
                deskPitchDeg = req.deskPitchDeg,
            )
        }
    }

    AndroidView(
        factory = { ctx -> RadialMenuView(ctx) },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            val req = request
            if (req == null || items.isEmpty() || rootWidthPx <= 0f || rootHeightPx <= 0f) {
                if (shownRequest.value != null) {
                    view.hide()
                    shownRequest.value = null
                }
                return@AndroidView
            }
            // Avoid resetting selection / isFirstUpAfterShow on every recomposition.
            val showKey = req to items.map { it.label to (it.subItems?.map { s -> s.label }) }
            if (shownRequest.value == showKey && view.visibility == View.VISIBLE) return@AndroidView
            shownRequest.value = showKey
            view.setItems(
                items = items,
                x = req.anchorX * rootWidthPx,
                y = req.anchorY * rootHeightPx,
                onSelected = { },
                onDismiss = {
                    shownRequest.value = null
                    LauncherContextMenuState.dismiss()
                },
            )
        },
    )
}

private fun appMenuItems(
    context: android.content.Context,
    app: LaunchableApp,
    isPinned: Boolean,
    showClear: Boolean,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
): List<RadialMenuItem> {
    val launchPanel = launchPanelForContextMenu()
    val launchMode = SpatialEmbedCapability.launchLabel(context, launchPanel)
    val dismiss = { LauncherContextMenuState.dismiss() }
    val items = mutableListOf(
        RadialMenuItem(
            label = context.getString(R.string.context_menu_open_app_with_mode, launchMode),
            iconRes = android.R.drawable.ic_menu_more,
        ) {
            dismiss()
            DeskLassoState.clearSelection()
            onLaunchApp(app)
        },
        RadialMenuItem(
            label = if (isPinned) {
                context.getString(R.string.context_menu_remove_from_hotseat)
            } else {
                context.getString(R.string.context_menu_add_to_hotseat)
            },
            iconRes = android.R.drawable.ic_menu_mylocation,
        ) {
            dismiss()
            onToggleHotseatPin(app)
        },
        RadialMenuItem(
            label = context.getString(R.string.context_menu_app_info),
            iconRes = android.R.drawable.ic_menu_info_details,
        ) {
            dismiss()
            AppSystemActions.openAppInfo(context, app)
        },
        RadialMenuItem(
            label = context.getString(R.string.context_menu_uninstall),
            iconRes = android.R.drawable.ic_menu_delete,
        ) {
            dismiss()
            AppSystemActions.requestUninstall(context, app)
        },
    )
    if (showClear) {
        items += RadialMenuItem(
            label = context.getString(R.string.context_menu_clear_selection),
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
        ) {
            dismiss()
            DeskLassoState.clearSelection()
        }
    }
    return items
}

private fun panelMenuItems(
    context: android.content.Context,
    panelId: String,
    onHidePanel: (String) -> Unit,
    onSnapPanelToGrid: (String) -> Unit,
): List<RadialMenuItem> {
    val dismiss = { LauncherContextMenuState.dismiss() }
    return listOf(
        RadialMenuItem(
            label = context.getString(R.string.context_menu_focus_panel),
            iconRes = android.R.drawable.ic_menu_view,
        ) {
            dismiss()
            CompanionPointerBus.setFocusedPanelId(panelId)
        },
        RadialMenuItem(
            label = context.getString(R.string.context_menu_snap_to_grid),
            iconRes = android.R.drawable.ic_menu_crop,
        ) {
            dismiss()
            onSnapPanelToGrid(panelId)
        },
        RadialMenuItem(
            label = context.getString(R.string.context_menu_hide_panel),
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
        ) {
            dismiss()
            onHidePanel(panelId)
        },
    )
}

/** BumpDesk [MenuManager.showLassoMenu] / empty-desktop structure. */
private fun desktopMenuItems(
    context: android.content.Context,
    selected: Set<String>,
    deskYawDeg: Float?,
    deskPitchDeg: Float?,
): List<RadialMenuItem> {
    val dismiss = { LauncherContextMenuState.dismiss() }
    if (selected.isNotEmpty()) {
        val selectedPile = HomeSpaceDeskState.piles.firstOrNull { it.id in selected }
        if (selectedPile != null) {
            return pileMenuItems(context, selectedPile, dismiss)
        }
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
        val selectedWidgets = HomeSpaceDeskState.placed.filter {
            it.app.componentKey in selected && it.app.kind == HomeSpaceDesk.Kind.WIDGET
        }
        fun scaleWidgets(factor: Float) {
            dismiss()
            HomeSpaceDeskState.scaleWidgets(selected, factor)
        }
        return buildList {
            if (selectedWidgets.isNotEmpty()) {
                add(
                    RadialMenuItem(
                        label = context.getString(R.string.context_menu_grow_widget),
                        iconRes = android.R.drawable.ic_menu_zoom,
                    ) { scaleWidgets(DeskWidgetUtils.SIZE_STEP) },
                )
                add(
                    RadialMenuItem(
                        label = context.getString(R.string.context_menu_shrink_widget),
                        iconRes = android.R.drawable.ic_menu_zoom,
                    ) { scaleWidgets(1f / DeskWidgetUtils.SIZE_STEP) },
                )
            }
            if (selected.size >= 2) {
                add(
                    RadialMenuItem(
                        label = context.getString(R.string.context_menu_move_group),
                        iconRes = android.R.drawable.ic_menu_mylocation,
                    ) {
                        dismiss()
                        DeskGroupMoveState.arm(selected)
                    },
                )
                add(
                    RadialMenuItem(
                        label = context.getString(R.string.context_menu_create_pile),
                        iconRes = android.R.drawable.ic_menu_add,
                        subItems = listOf(
                            RadialMenuItem(
                                label = context.getString(R.string.context_menu_arrange_folder),
                                iconRes = android.R.drawable.ic_menu_agenda,
                            ) {
                                dismiss()
                                HomeSpaceDeskState.createPile(selected, DeskPileMode.FOLDER)
                                DeskLassoState.clearSelection()
                            },
                            RadialMenuItem(
                                label = context.getString(R.string.context_menu_arrange_stack),
                                iconRes = android.R.drawable.ic_menu_sort_by_size,
                            ) {
                                dismiss()
                                HomeSpaceDeskState.createPile(selected, DeskPileMode.STACK)
                                DeskLassoState.clearSelection()
                            },
                        ),
                    ),
                )
                add(
                    RadialMenuItem(
                        label = context.getString(R.string.context_menu_layout),
                        iconRes = android.R.drawable.ic_menu_sort_by_size,
                        subItems = listOf(
                            RadialMenuItem(
                                label = context.getString(R.string.context_menu_arrange_grid),
                                iconRes = android.R.drawable.ic_menu_sort_by_size,
                            ) { arrange(DeskArrangeMode.GRID) },
                            RadialMenuItem(
                                label = context.getString(R.string.context_menu_arrange_row),
                                iconRes = android.R.drawable.ic_menu_sort_alphabetically,
                            ) { arrange(DeskArrangeMode.ROW) },
                            RadialMenuItem(
                                label = context.getString(R.string.context_menu_arrange_column),
                                iconRes = android.R.drawable.ic_menu_sort_alphabetically,
                            ) { arrange(DeskArrangeMode.COLUMN) },
                        ),
                    ),
                )
            }
            add(
                RadialMenuItem(
                    label = context.getString(R.string.context_menu_clear_selection),
                    iconRes = android.R.drawable.ic_menu_close_clear_cancel,
                ) {
                    dismiss()
                    DeskLassoState.clearSelection()
                },
            )
            add(
                RadialMenuItem(
                    label = context.getString(R.string.context_menu_remove_from_desktop),
                    iconRes = android.R.drawable.ic_menu_delete,
                ) {
                    dismiss()
                    HomeSpaceDeskState.removeByKeys(selected)
                    DeskLassoState.clearSelection()
                },
            )
        }
    }
    return listOf(
        RadialMenuItem(
            label = context.getString(R.string.context_menu_add_widget),
            iconRes = android.R.drawable.ic_menu_manage,
        ) {
            dismiss()
            DeskWidgetController.openPicker(
                yawDeg = deskYawDeg ?: 0f,
                pitchDeg = deskPitchDeg ?: 0f,
            )
        },
        RadialMenuItem(
            label = context.getString(R.string.context_menu_open_all_apps),
            iconRes = android.R.drawable.ic_menu_search,
        ) {
            dismiss()
            GlassesSessionState.showAllAppsOverlay()
        },
    )
}

/** BumpDesk [MenuManager.showPileMenu]. */
private fun pileMenuItems(
    context: android.content.Context,
    pile: DeskPile,
    dismiss: () -> Unit,
): List<RadialMenuItem> {
    val fanLabel = if (pile.fannedOut) {
        context.getString(R.string.context_menu_pile_collapse_fan)
    } else {
        context.getString(R.string.context_menu_pile_fan)
    }
    val expandLabel = if (pile.expanded) {
        context.getString(R.string.context_menu_pile_collapse)
    } else {
        context.getString(R.string.context_menu_pile_expand)
    }
    return listOf(
        RadialMenuItem(
            label = expandLabel,
            iconRes = android.R.drawable.ic_menu_view,
        ) {
            dismiss()
            HomeSpaceDeskState.togglePileExpanded(pile.id)
            DeskLassoState.clearSelection()
        },
        RadialMenuItem(
            label = fanLabel,
            iconRes = android.R.drawable.ic_menu_sort_alphabetically,
        ) {
            dismiss()
            HomeSpaceDeskState.togglePileFan(pile.id)
            DeskLassoState.clearSelection()
        },
        RadialMenuItem(
            label = context.getString(R.string.context_menu_pile_break_apart),
            iconRes = android.R.drawable.ic_menu_revert,
        ) {
            dismiss()
            HomeSpaceDeskState.breakPile(pile.id)
            DeskLassoState.clearSelection()
        },
        RadialMenuItem(
            label = context.getString(R.string.context_menu_clear_selection),
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
        ) {
            dismiss()
            DeskLassoState.clearSelection()
        },
    )
}

private fun launchPanelForContextMenu(): PanelState {
    val focusedId = CompanionPointerBus.focusedPanelId.value
    return when (focusedId) {
        "empty_slot" -> PanelState(id = "empty_slot", kind = PanelKind.EMPTY_SLOT, visible = true)
        else -> PanelState(id = "full_window", kind = PanelKind.EMPTY_SLOT)
    }
}
