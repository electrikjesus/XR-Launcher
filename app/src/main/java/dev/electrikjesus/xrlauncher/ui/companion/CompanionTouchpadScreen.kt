package dev.electrikjesus.xrlauncher.ui.companion

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import androidx.compose.runtime.rememberCoroutineScope
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import androidx.compose.ui.text.style.TextOverflow
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceLookOffset
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.launch

private enum class CompanionTab(val labelRes: Int) {
    Display(R.string.companion_tab_display),
    Input(R.string.companion_tab_input),
    Workspace(R.string.companion_tab_workspace),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionTouchpadScreen(
    workspaceRepository: WorkspaceRepository,
    apps: List<LaunchableApp> = emptyList(),
    onLaunchAppOnGlasses: (LaunchableApp) -> Unit = {},
    motionAvailable: Boolean = true,
    isCalibrating: Boolean = false,
    onCalibrate: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val appearance = workspace?.appearance?.clamped() ?: WorkspaceAppearance.default()
    val scope = rememberCoroutineScope()
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val motionEnabled by CompanionPointerBus.motionControlEnabled.collectAsState()
    val motionSensitivity by CompanionPointerBus.motionSensitivity.collectAsState()
    val touchpadSensitivity by CompanionPointerBus.touchpadSensitivity.collectAsState()
    val focusedPanelId by CompanionPointerBus.focusedPanelId.collectAsState()
    val launcherForeground by GlassesSessionState.launcherForegroundFlow.collectAsState()
    val textEntryActive by CompanionPointerBus.textEntryActiveFlow.collectAsState()
    var precisionPointer by remember { mutableStateOf(false) }
    val touchpadClickSuppressed = textEntryActive || precisionPointer
    val context = LocalContext.current
    val desktopPointerReady = DisplayPointerInjector.isAvailable
    var selectedTab by remember { mutableIntStateOf(CompanionTab.Display.ordinal) }

    val statusHint = when {
        !desktopPointerReady -> stringResource(R.string.control_mode_desktop_setup_hint)
        textEntryActive -> stringResource(R.string.companion_text_entry_hint)
        touchpadClickSuppressed -> stringResource(R.string.precision_pointer_on_hint)
        launcherForeground -> stringResource(R.string.companion_launcher_foreground_hint)
        else -> stringResource(R.string.companion_pointer_active_hint)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.companion_touchpad),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // Top half — controls (tabs); height fixed at 50% so touchpad never shrinks.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShowLauncherOnGlassesButton(
                        onClick = { DisplayLaunchHelper.showLauncherOnGlasses(context) },
                        modifier = Modifier.weight(1f),
                    )
                    AllAppsOnGlassesButton(
                        onClick = { DisplayLaunchHelper.openAllAppsOnGlasses(context) },
                        modifier = Modifier.weight(1f),
                    )
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    CompanionTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(stringResource(tab.labelRes)) },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        CompanionTab.Display -> Icons.Default.TouchApp
                                        CompanionTab.Input -> Icons.Default.SettingsInputComponent
                                        CompanionTab.Workspace -> Icons.Default.Dashboard
                                    },
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.companion_status_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = statusHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        when (CompanionTab.entries[selectedTab]) {
                            CompanionTab.Display -> DisplayTabContent(
                                desktopPointerReady = desktopPointerReady,
                                precisionPointer = precisionPointer,
                                onPrecisionPointerChange = {
                                    precisionPointer = it
                                    CompanionPointerBus.setManualPrecisionPointer(it)
                                },
                                onOpenAccessibilitySettings = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                apps = apps,
                                onLaunchAppOnGlasses = onLaunchAppOnGlasses,
                            )
                            CompanionTab.Input -> InputTabContent(
                                motionAvailable = motionAvailable,
                                motionEnabled = motionEnabled,
                                motionSensitivity = motionSensitivity,
                                touchpadSensitivity = touchpadSensitivity,
                                isCalibrating = isCalibrating,
                                onCalibrate = onCalibrate,
                            )
                            CompanionTab.Workspace -> WorkspaceTabContent(
                                appearance = appearance,
                                onUiScaleChange = { scale ->
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(uiScale = scale),
                                        )
                                    }
                                },
                                onPanelGapChange = { gap ->
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(panelGapDp = gap),
                                        )
                                    }
                                },
                                onWrapCurvatureChange = { curvature ->
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(wrapCurvature = curvature),
                                        )
                                    }
                                },
                                onWorkspaceWidthChange = { width ->
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(workspaceWidth = width),
                                        )
                                    }
                                },
                                onWorkspaceHeightChange = { height ->
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(workspaceHeight = height),
                                        )
                                    }
                                },
                                onLookYawChange = { yaw ->
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(lookYawDegrees = yaw),
                                        )
                                    }
                                },
                                onLookPitchChange = { pitch ->
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(lookPitchDegrees = pitch),
                                        )
                                    }
                                },
                                onRecenterLook = {
                                    WorkspaceLookOffset.reset()
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(
                                                lookYawDegrees = 0f,
                                                lookPitchDegrees = 0f,
                                            ),
                                        )
                                    }
                                },
                                onResetAppearance = {
                                    WorkspaceLookOffset.reset()
                                    scope.launch {
                                        workspaceRepository.resetLayoutDefaults()
                                    }
                                },
                                cursorHoveredLabel = cursor.hoveredLabel,
                                focusedPanelId = focusedPanelId,
                                launcherForeground = launcherForeground,
                            )
                        }
                    }
                }
            }

            // Bottom half — touchpad + click buttons (always 50% of content area).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompanionTouchpadSurface(
                    motionEnabled = motionEnabled,
                    desktopPointerReady = desktopPointerReady,
                    touchpadClickSuppressed = touchpadClickSuppressed,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                CompanionPointerButtonsRow(
                    desktopPointerReady = desktopPointerReady,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AllAppsOnGlassesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = stringResource(R.string.all_apps_on_glasses),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun ShowLauncherOnGlassesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = stringResource(R.string.show_launcher_on_glasses),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun DisplayTabContent(
    desktopPointerReady: Boolean,
    precisionPointer: Boolean,
    onPrecisionPointerChange: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    apps: List<LaunchableApp>,
    onLaunchAppOnGlasses: (LaunchableApp) -> Unit,
) {
    if (!desktopPointerReady) {
        OutlinedButton(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.enable_desktop_pointer))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.precision_pointer),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (precisionPointer) {
                        stringResource(R.string.precision_pointer_on_hint)
                    } else {
                        stringResource(R.string.precision_pointer_off_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = precisionPointer,
                onCheckedChange = onPrecisionPointerChange,
            )
        }
    }

    if (apps.isNotEmpty()) {
        CompanionAllAppsPicker(
            apps = apps,
            onLaunchApp = onLaunchAppOnGlasses,
        )
    }
}

@Composable
private fun InputTabContent(
    motionAvailable: Boolean,
    motionEnabled: Boolean,
    motionSensitivity: Float,
    touchpadSensitivity: Float,
    isCalibrating: Boolean,
    onCalibrate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.motion_control),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = if (motionEnabled) {
                    stringResource(R.string.motion_control_on_hint)
                } else {
                    stringResource(R.string.motion_control_off_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = motionEnabled,
            onCheckedChange = { CompanionPointerBus.setMotionControlEnabled(it) },
            enabled = motionAvailable,
        )
    }

    if (motionEnabled) {
        Text(
            text = stringResource(R.string.motion_sensitivity),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = motionSensitivity,
            onValueChange = { CompanionPointerBus.setMotionSensitivity(it) },
            valueRange = 0.25f..3f,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { CompanionPointerBus.recenterCursor() },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.recenter))
            }
            OutlinedButton(
                onClick = onCalibrate,
                enabled = !isCalibrating,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    if (isCalibrating) {
                        stringResource(R.string.calibrating)
                    } else {
                        stringResource(R.string.calibrate)
                    },
                )
            }
        }
    } else {
        Text(
            text = stringResource(R.string.touchpad_sensitivity),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = touchpadSensitivity,
            onValueChange = { CompanionPointerBus.setTouchpadSensitivity(it) },
            valueRange = 0.25f..3f,
        )
        OutlinedButton(
            onClick = { CompanionPointerBus.recenterCursor() },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.recenter))
        }
    }
}

@Composable
private fun WorkspaceTabContent(
    appearance: WorkspaceAppearance,
    onUiScaleChange: (Float) -> Unit,
    onPanelGapChange: (Float) -> Unit,
    onWrapCurvatureChange: (Float) -> Unit,
    onWorkspaceWidthChange: (Float) -> Unit,
    onWorkspaceHeightChange: (Float) -> Unit,
    onLookYawChange: (Float) -> Unit,
    onLookPitchChange: (Float) -> Unit,
    onRecenterLook: () -> Unit,
    onResetAppearance: () -> Unit,
    cursorHoveredLabel: String?,
    focusedPanelId: String?,
    launcherForeground: Boolean,
) {
    Text(
        text = stringResource(R.string.workspace_appearance_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = stringResource(R.string.workspace_ui_scale_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(
            R.string.workspace_ui_scale,
        ) + " · ${(appearance.uiScale * 100).toInt()}%",
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
        modifier = Modifier.padding(top = 4.dp),
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
        modifier = Modifier.padding(top = 8.dp),
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
        text = stringResource(R.string.workspace_look_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
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
    OutlinedButton(
        onClick = onResetAppearance,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(stringResource(R.string.workspace_appearance_reset))
    }

    if (cursorHoveredLabel != null) {
        Text(
            text = stringResource(R.string.cursor_over, cursorHoveredLabel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    if (focusedPanelId != null && launcherForeground) {
        Text(
            text = stringResource(
                R.string.workspace_focused_panel,
                focusedPanelLabel(focusedPanelId),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedButton(
            onClick = {
                CompanionPointerBus.focusNextPanel(
                    listOf(
                        "widget_clock",
                        "widget_calendar",
                        "app_drawer",
                        "hotseat",
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.workspace_focus_next))
        }
        OutlinedButton(
            onClick = {
                CompanionPointerBus.setFocusedPanelId("app_drawer")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.all_apps_focus_drawer))
        }
    } else if (cursorHoveredLabel == null) {
        Text(
            text = stringResource(R.string.companion_pointer_active_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun focusedPanelLabel(panelId: String): String = when (panelId) {
    "widget_clock" -> stringResource(R.string.workspace_panel_clock)
    "widget_calendar" -> stringResource(R.string.workspace_panel_calendar)
    "app_drawer" -> stringResource(R.string.workspace_panel_drawer)
    "hotseat" -> stringResource(R.string.workspace_panel_hotseat)
    "empty_slot" -> stringResource(R.string.workspace_empty_slot)
    else -> panelId
}
