package dev.electrikjesus.xrlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsGridConfig
import dev.electrikjesus.xrlauncher.core.launcher.LauncherReturnBubbleStore
import dev.electrikjesus.xrlauncher.core.launcher.SystemWallpaperLoader
import dev.electrikjesus.xrlauncher.core.workspace.DeskGrid
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWallpaperChoice

@Composable
fun WorkspaceAppearanceSettingsSection(
    appearance: WorkspaceAppearance,
    headTrackingActive: Boolean,
    onUiScaleChange: (Float) -> Unit,
    onPanelGapChange: (Float) -> Unit,
    onWrapCurvatureChange: (Float) -> Unit,
    onWorkspaceWidthChange: (Float) -> Unit,
    onWorkspaceHeightChange: (Float) -> Unit,
    onLookYawChange: (Float) -> Unit,
    onLookPitchChange: (Float) -> Unit,
    onLookModeChange: (GlassesLookMode) -> Unit,
    onRecenterLook: () -> Unit,
    onResetAppearance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.workspace_ui_scale_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.workspace_ui_scale) +
                " · ${(appearance.uiScale * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = appearance.uiScale,
            onValueChange = onUiScaleChange,
            valueRange = WorkspaceAppearance.MIN_UI_SCALE..WorkspaceAppearance.MAX_UI_SCALE,
        )
        Text(
            text = stringResource(R.string.workspace_panel_gap_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.workspace_panel_gap) +
                " · ${appearance.panelGapDp.toInt()}dp",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = appearance.panelGapDp,
            onValueChange = onPanelGapChange,
            valueRange = WorkspaceAppearance.MIN_PANEL_GAP_DP..WorkspaceAppearance.MAX_PANEL_GAP_DP,
        )

        Text(
            text = stringResource(R.string.workspace_wrap_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.workspace_wrap_curvature_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.workspace_wrap_curvature) +
                " · ${(appearance.wrapCurvature * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = appearance.wrapCurvature,
            onValueChange = onWrapCurvatureChange,
            valueRange = WorkspaceAppearance.MIN_WRAP_CURVATURE..WorkspaceAppearance.MAX_WRAP_CURVATURE,
        )
        Text(
            text = stringResource(R.string.workspace_span_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.workspace_span_width) +
                " · ${(appearance.workspaceWidth * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = appearance.workspaceWidth,
            onValueChange = onWorkspaceWidthChange,
            valueRange = WorkspaceAppearance.MIN_WORKSPACE_SPAN..WorkspaceAppearance.MAX_WORKSPACE_SPAN,
        )
        Text(
            text = stringResource(R.string.workspace_span_height) +
                " · ${(appearance.workspaceHeight * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = appearance.workspaceHeight,
            onValueChange = onWorkspaceHeightChange,
            valueRange = WorkspaceAppearance.MIN_WORKSPACE_SPAN..WorkspaceAppearance.MAX_WORKSPACE_SPAN,
        )

        Text(
            text = if (headTrackingActive) {
                stringResource(R.string.workspace_look_head_tracking_hint)
            } else {
                stringResource(R.string.workspace_look_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.workspace_look_mode_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { onLookModeChange(GlassesLookMode.GRADIENT) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = appearance.lookMode != GlassesLookMode.GRADIENT,
        ) {
            Text(stringResource(R.string.workspace_look_mode_gradient))
        }
        OutlinedButton(
            onClick = { onLookModeChange(GlassesLookMode.GESTURE) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = appearance.lookMode != GlassesLookMode.GESTURE,
        ) {
            Text(stringResource(R.string.workspace_look_mode_gesture))
        }
        OutlinedButton(
            onClick = { onLookModeChange(GlassesLookMode.FPS) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = appearance.lookMode != GlassesLookMode.FPS,
        ) {
            Text(stringResource(R.string.workspace_look_mode_fps))
        }
        if (!headTrackingActive) {
            Text(
                text = stringResource(R.string.workspace_look_yaw) +
                    " · ${appearance.lookYawDegrees.toInt()}°",
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = appearance.lookYawDegrees,
                onValueChange = onLookYawChange,
                valueRange = WorkspaceAppearance.MIN_LOOK_YAW..WorkspaceAppearance.MAX_LOOK_YAW,
            )
            Text(
                text = stringResource(R.string.workspace_look_pitch) +
                    " · ${appearance.lookPitchDegrees.toInt()}°",
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(
                value = appearance.lookPitchDegrees,
                onValueChange = onLookPitchChange,
                valueRange = WorkspaceAppearance.MIN_LOOK_PITCH..WorkspaceAppearance.MAX_LOOK_PITCH,
            )
            OutlinedButton(
                onClick = onRecenterLook,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.workspace_look_recenter))
            }
        }
        OutlinedButton(
            onClick = onResetAppearance,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.workspace_appearance_reset))
        }
    }
}

@Composable
fun AllAppsGridSettingsSection(
    columns: Int,
    rows: Int,
    onColumnsChange: (Int) -> Unit,
    onRowsChange: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_all_apps_grid_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_all_apps_columns, columns),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = columns.toFloat(),
            onValueChange = { onColumnsChange(it.toInt()) },
            valueRange = AllAppsGridConfig.MIN_COLUMNS.toFloat()..AllAppsGridConfig.MAX_COLUMNS.toFloat(),
            steps = AllAppsGridConfig.MAX_COLUMNS - AllAppsGridConfig.MIN_COLUMNS - 1,
        )
        Text(
            text = stringResource(R.string.settings_all_apps_rows, rows),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = rows.toFloat(),
            onValueChange = { onRowsChange(it.toInt()) },
            valueRange = AllAppsGridConfig.MIN_ROWS.toFloat()..AllAppsGridConfig.MAX_ROWS.toFloat(),
            steps = AllAppsGridConfig.MAX_ROWS - AllAppsGridConfig.MIN_ROWS - 1,
        )
        Text(
            text = stringResource(R.string.settings_all_apps_page_size, columns * rows),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.settings_all_apps_reset))
        }
    }
}

@Composable
fun ReturnBubbleSettingsSection(
    sizeDp: Float,
    onSizeDpChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_return_bubble_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_return_bubble_size, sizeDp.toInt()),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = sizeDp,
            onValueChange = onSizeDpChange,
            valueRange = LauncherReturnBubbleStore.MIN_SIZE_DP..LauncherReturnBubbleStore.MAX_SIZE_DP,
        )
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.settings_return_bubble_reset))
        }
    }
}

@Composable
fun DeskGridSettingsSection(
    snapToGrid: Boolean,
    showGridOnMove: Boolean,
    gridScale: Float,
    onSnapToGridChange: (Boolean) -> Unit,
    onShowGridOnMoveChange: (Boolean) -> Unit,
    onGridScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_desk_grid_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { onSnapToGridChange(!snapToGrid) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                stringResource(
                    if (snapToGrid) {
                        R.string.settings_desk_snap_to_grid_on
                    } else {
                        R.string.settings_desk_snap_to_grid_off
                    },
                ),
            )
        }
        OutlinedButton(
            onClick = { onShowGridOnMoveChange(!showGridOnMove) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                stringResource(
                    if (showGridOnMove) {
                        R.string.settings_desk_show_grid_on_move_on
                    } else {
                        R.string.settings_desk_show_grid_on_move_off
                    },
                ),
            )
        }
        Text(
            text = stringResource(
                R.string.settings_desk_grid_scale,
                (gridScale * 100f).toInt(),
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = gridScale,
            onValueChange = onGridScaleChange,
            valueRange = DeskGrid.MIN_GRID_SCALE..DeskGrid.MAX_GRID_SCALE,
        )
    }
}

@Composable
fun PointerSensitivitySettingsSection(
    motionSensitivity: Float,
    touchpadSensitivity: Float,
    onMotionSensitivityChange: (Float) -> Unit,
    onTouchpadSensitivityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_pointer_sensitivity_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.touchpad_sensitivity),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = touchpadSensitivity,
            onValueChange = onTouchpadSensitivityChange,
            valueRange = 0.25f..3f,
        )
        Text(
            text = stringResource(R.string.motion_sensitivity),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = motionSensitivity,
            onValueChange = onMotionSensitivityChange,
            valueRange = 0.25f..3f,
        )
    }
}

@Composable
fun WorkspaceWallpaperSettingsSection(
    wallpaperChoice: WorkspaceWallpaperChoice,
    onWallpaperChoiceChange: (WorkspaceWallpaperChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasWallpaperAccess by remember {
        mutableStateOf(SystemWallpaperLoader.canReadSystemWallpaper(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasWallpaperAccess = SystemWallpaperLoader.canReadSystemWallpaper(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_wallpaper_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasWallpaperAccess) {
            Text(
                text = stringResource(R.string.settings_wallpaper_access_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(SystemWallpaperLoader.allFilesAccessSettingsIntent(context))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.settings_wallpaper_grant_access))
            }
        }
        WorkspaceWallpaperChoice.entries.forEach { choice ->
            val label = when (choice) {
                WorkspaceWallpaperChoice.SYSTEM -> stringResource(R.string.settings_wallpaper_system)
                WorkspaceWallpaperChoice.GRADIENT_TWILIGHT -> stringResource(R.string.settings_wallpaper_twilight)
                WorkspaceWallpaperChoice.GRADIENT_AURORA -> stringResource(R.string.settings_wallpaper_aurora)
                WorkspaceWallpaperChoice.GRADIENT_EMISSIVE -> stringResource(R.string.settings_wallpaper_emissive)
            }
            OutlinedButton(
                onClick = { onWallpaperChoiceChange(choice) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                enabled = wallpaperChoice != choice,
            ) {
                Text(
                    text = if (wallpaperChoice == choice) {
                        stringResource(R.string.settings_wallpaper_selected, label)
                    } else {
                        label
                    },
                )
            }
        }
    }
}

@Composable
fun HiddenPanelsSettingsSection(
    hiddenPanelIds: List<String>,
    onRestorePanel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hiddenPanelIds.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_hidden_panels_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        hiddenPanelIds.forEach { panelId ->
            OutlinedButton(
                onClick = { onRestorePanel(panelId) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.settings_restore_panel, panelId))
            }
        }
    }
}
