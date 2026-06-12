package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.capability.SpatialEmbedCapability
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.launcher.AppSystemActions
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuTarget
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
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
    val menuWidthPx = with(density) { 240.dp.toPx() }
    val anchorXPx = request!!.anchorX * rootWidthPx
    val anchorYPx = request!!.anchorY * rootHeightPx
    val offsetX = (anchorXPx - menuWidthPx / 2f).coerceIn(8f, rootWidthPx - menuWidthPx - 8f)
    val offsetY = (anchorYPx + with(density) { 12.dp.toPx() })
        .coerceIn(8f, rootHeightPx - with(density) { 280.dp.toPx() })

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
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .widthIn(min = 200.dp, max = 280.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                when (val target = request!!.target) {
                    is LauncherContextMenuTarget.App -> AppContextMenuItems(
                        app = target.app,
                        isPinned = target.isPinned,
                        onLaunchApp = onLaunchApp,
                        onToggleHotseatPin = onToggleHotseatPin,
                        onDismiss = { LauncherContextMenuState.dismiss() },
                        context = context,
                    )
                    is LauncherContextMenuTarget.Panel -> PanelContextMenuItems(
                        panelId = target.panelId,
                        kind = target.kind,
                        onHidePanel = onHidePanel,
                        onSnapPanelToGrid = onSnapPanelToGrid,
                        onDismiss = { LauncherContextMenuState.dismiss() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppContextMenuItems(
    app: LaunchableApp,
    isPinned: Boolean,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleHotseatPin: (LaunchableApp) -> Unit,
    onDismiss: () -> Unit,
    context: android.content.Context,
) {
    ContextMenuHeader(app.label)
    val launchPanel = launchPanelForContextMenu()
    val launchMode = SpatialEmbedCapability.launchLabel(context, launchPanel)
    ContextMenuItem(
        label = stringResource(R.string.context_menu_open_app_with_mode, launchMode),
        onClick = {
            onDismiss()
            onLaunchApp(app)
        },
    )
    ContextMenuItem(
        label = if (isPinned) {
            stringResource(R.string.context_menu_remove_from_hotseat)
        } else {
            stringResource(R.string.context_menu_add_to_hotseat)
        },
        onClick = {
            onDismiss()
            onToggleHotseatPin(app)
        },
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    ContextMenuItem(
        label = stringResource(R.string.context_menu_app_info),
        onClick = {
            onDismiss()
            AppSystemActions.openAppInfo(context, app)
        },
    )
    ContextMenuItem(
        label = stringResource(R.string.context_menu_uninstall),
        onClick = {
            onDismiss()
            AppSystemActions.requestUninstall(context, app)
        },
    )
}

@Composable
private fun PanelContextMenuItems(
    panelId: String,
    kind: PanelKind,
    onHidePanel: (String) -> Unit,
    onSnapPanelToGrid: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ContextMenuHeader(panelTitle(panelId, kind))
    ContextMenuItem(
        label = stringResource(R.string.context_menu_focus_panel),
        onClick = {
            onDismiss()
            CompanionPointerBus.setFocusedPanelId(panelId)
        },
    )
    ContextMenuItem(
        label = stringResource(R.string.context_menu_snap_to_grid),
        onClick = {
            onDismiss()
            onSnapPanelToGrid(panelId)
        },
    )
    ContextMenuItem(
        label = stringResource(R.string.context_menu_hide_panel),
        onClick = {
            onDismiss()
            onHidePanel(panelId)
        },
    )
}

@Composable
private fun ContextMenuHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ContextMenuItem(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
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
