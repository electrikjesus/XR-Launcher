package dev.electrikjesus.xrlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.rayneo.HeadTrackingMovementScales
import dev.electrikjesus.xrlauncher.core.input.rayneo.HeadTrackingSensitivityStore
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsGridConfigStore
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLookOffset
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.launch

/**
 * @param homeSpaceOnly When true (immersive 3D Settings dialog), hide companion / 2D cylinder
 * options that do nothing on Home Space. Phone [SettingsActivity] keeps the full list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    workspaceRepository: WorkspaceRepository,
    onNavigateBack: () -> Unit,
    onShowOnboarding: () -> Unit = {},
    homeSpaceOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val appearance = workspace?.appearance?.clamped() ?: WorkspaceAppearance.default()
    val gridDimensions by AllAppsGridConfigStore.dimensions.collectAsState()
    val headTrackingScales by CompanionPointerBus.glassesImuMovementScales.collectAsState()
    val motionSensitivity by CompanionPointerBus.motionSensitivity.collectAsState()
    val touchpadSensitivity by CompanionPointerBus.touchpadSensitivity.collectAsState()
    val headTrackingActive = GlassesSessionState.xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!homeSpaceOnly) {
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_how_to_section))
                    OutlinedButton(
                        onClick = onShowOnboarding,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(stringResource(R.string.onboarding_show_again))
                    }
                }
            }
            item {
                SettingsSectionTitle(stringResource(R.string.settings_wallpaper_section))
                WorkspaceWallpaperSettingsSection(
                    wallpaperChoice = appearance.wallpaperChoice,
                    onWallpaperChoiceChange = { choice ->
                        scope.launch {
                            workspaceRepository.updateAppearance(appearance.copy(wallpaperChoice = choice))
                        }
                    },
                )
            }
            item {
                SettingsSectionTitle(stringResource(R.string.settings_all_apps_section))
                AllAppsGridSettingsSection(
                    columns = gridDimensions.columns,
                    rows = gridDimensions.rows,
                    onColumnsChange = { value ->
                        AllAppsGridConfigStore.saveColumns(context, value)
                        AllAppsPaginationState.goToPage(0)
                    },
                    onRowsChange = { value ->
                        AllAppsGridConfigStore.saveRows(context, value)
                        AllAppsPaginationState.goToPage(0)
                    },
                    onReset = { AllAppsGridConfigStore.resetToDefaults(context) },
                )
            }
            if (homeSpaceOnly) {
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_look_section))
                    HomeSpaceLookSettingsSection(
                        lookMode = appearance.lookMode,
                        onLookModeChange = { mode ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(lookMode = mode))
                            }
                            GlassesLookMode.preference = mode
                        },
                        onRecenterLook = {
                            GlassesHomeLook.lookYawDegrees = 0f
                            CompanionPointerBus.recenterCursor()
                            scope.launch {
                                workspaceRepository.updateAppearance(
                                    appearance.copy(lookYawDegrees = 0f, lookPitchDegrees = 0f),
                                )
                            }
                        },
                        onResetAppearance = {
                            scope.launch { workspaceRepository.resetLayoutDefaults() }
                        },
                    )
                }
            } else {
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_workspace_section))
                    WorkspaceAppearanceSettingsSection(
                        appearance = appearance,
                        headTrackingActive = headTrackingActive,
                        onUiScaleChange = { scale ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(uiScale = scale))
                            }
                        },
                        onPanelGapChange = { gap ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(panelGapDp = gap))
                            }
                        },
                        onWrapCurvatureChange = { value ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(wrapCurvature = value))
                            }
                        },
                        onWorkspaceWidthChange = { value ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(workspaceWidth = value))
                            }
                        },
                        onWorkspaceHeightChange = { value ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(workspaceHeight = value))
                            }
                        },
                        onLookYawChange = { value ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(lookYawDegrees = value))
                            }
                        },
                        onLookPitchChange = { value ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(lookPitchDegrees = value))
                            }
                        },
                        onLookModeChange = { mode ->
                            scope.launch {
                                workspaceRepository.updateAppearance(appearance.copy(lookMode = mode))
                            }
                            GlassesLookMode.preference = mode
                        },
                        onRecenterLook = {
                            scope.launch {
                                workspaceRepository.updateAppearance(
                                    appearance.copy(lookYawDegrees = 0f, lookPitchDegrees = 0f),
                                )
                                WorkspaceLookOffset.reset()
                            }
                        },
                        onResetAppearance = {
                            scope.launch { workspaceRepository.resetLayoutDefaults() }
                        },
                    )
                }
            }
            item {
                val hiddenPanels = workspace?.panels?.filter { !it.visible }.orEmpty()
                if (hiddenPanels.isNotEmpty()) {
                    SettingsSectionTitle(stringResource(R.string.settings_hidden_panels_section))
                    HiddenPanelsSettingsSection(
                        hiddenPanelIds = hiddenPanels.map { it.id },
                        onRestorePanel = { panelId ->
                            scope.launch { workspaceRepository.setPanelVisible(panelId, visible = true) }
                        },
                    )
                }
            }
            if (!homeSpaceOnly) {
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_pointer_section))
                    PointerSensitivitySettingsSection(
                        motionSensitivity = motionSensitivity,
                        touchpadSensitivity = touchpadSensitivity,
                        onMotionSensitivityChange = { CompanionPointerBus.setMotionSensitivity(it) },
                        onTouchpadSensitivityChange = { CompanionPointerBus.setTouchpadSensitivity(it) },
                    )
                }
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_head_tracking_section))
                    HeadTrackingScaleSettingsSection(
                        scales = headTrackingScales,
                        onYawChange = { CompanionPointerBus.setGlassesImuYawScale(it) },
                        onPitchChange = { CompanionPointerBus.setGlassesImuPitchScale(it) },
                        onReset = { CompanionPointerBus.resetGlassesImuMovementScales() },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSpaceLookSettingsSection(
    lookMode: GlassesLookMode,
    onLookModeChange: (GlassesLookMode) -> Unit,
    onRecenterLook: () -> Unit,
    onResetAppearance: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_look_home_space_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { onLookModeChange(GlassesLookMode.GRADIENT) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = lookMode != GlassesLookMode.GRADIENT,
        ) {
            Text(stringResource(R.string.workspace_look_mode_gradient))
        }
        OutlinedButton(
            onClick = { onLookModeChange(GlassesLookMode.FPS) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = lookMode != GlassesLookMode.FPS,
        ) {
            Text(stringResource(R.string.workspace_look_mode_fps))
        }
        OutlinedButton(
            onClick = onRecenterLook,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.workspace_look_recenter))
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
private fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

@Composable
private fun HeadTrackingScaleSettingsSection(
    scales: HeadTrackingMovementScales,
    onYawChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_head_tracking_scales_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.xr_head_tracking_yaw_scale,
                (scales.yawScale * 100).toInt(),
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = scales.yawScale,
            onValueChange = onYawChange,
            valueRange = HeadTrackingSensitivityStore.MIN..HeadTrackingSensitivityStore.MAX,
        )
        Text(
            text = stringResource(
                R.string.xr_head_tracking_pitch_scale,
                (scales.pitchScale * 100).toInt(),
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = scales.pitchScale,
            onValueChange = onPitchChange,
            valueRange = HeadTrackingSensitivityStore.MIN..HeadTrackingSensitivityStore.MAX,
        )
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.xr_head_tracking_scale_reset))
        }
    }
}
