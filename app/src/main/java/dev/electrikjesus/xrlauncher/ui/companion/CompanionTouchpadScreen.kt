package dev.electrikjesus.xrlauncher.ui.companion

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import dev.electrikjesus.xrlauncher.core.display.GlassesControlMode
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.input.rayneo.RayNeoHeadTrackingController
import dev.electrikjesus.xrlauncher.core.input.rayneo.RayNeoHeadTrackingState
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.settings.SettingsActivity
import dev.electrikjesus.xrlauncher.ui.workspace.CompanionAllAppsPageControls
import kotlinx.coroutines.launch

private enum class CompanionTab(val labelRes: Int) {
    Display(R.string.companion_tab_display),
    Input(R.string.companion_tab_input),
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val motionEnabled by CompanionPointerBus.motionControlEnabled.collectAsState()
    val xrInputMode by GlassesSessionState.xrInputModeFlow.collectAsState()
    val headTrackingState by RayNeoHeadTrackingController.state.collectAsState()
    val headTrackingError by RayNeoHeadTrackingController.lastError.collectAsState()
    val rayNeoUsbAttached = GlassesSessionState.rayNeoUsbAttached ||
        RayNeoHeadTrackingController.isRayNeoAttached(context)
    val focusedPanelId by CompanionPointerBus.focusedPanelId.collectAsState()
    val launcherForeground by GlassesSessionState.launcherForegroundFlow.collectAsState()
    val textEntryActive by CompanionPointerBus.textEntryActiveFlow.collectAsState()
    var precisionPointer by remember { mutableStateOf(false) }
    val touchpadClickSuppressed = textEntryActive || precisionPointer
    val desktopPointerReady = DisplayPointerInjector.isAvailable
    val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()
    var selectedTab by remember { mutableIntStateOf(CompanionTab.Display.ordinal) }
    var showControls by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val statusHint = when {
        !desktopPointerReady -> stringResource(R.string.control_mode_desktop_setup_hint)
        textEntryActive -> stringResource(R.string.companion_text_entry_hint)
        touchpadClickSuppressed -> stringResource(R.string.precision_pointer_on_hint)
        launcherForeground -> stringResource(R.string.companion_launcher_foreground_hint)
        else -> stringResource(R.string.companion_pointer_active_hint)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showControls = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.companion_show_controls),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (allAppsOverlayVisible) {
                FilledTonalButton(
                    onClick = { DisplayLaunchHelper.closeAllAppsOnGlasses() },
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.all_apps_close_on_glasses))
                }
            }
        }

        CompanionTouchpadSurface(
            motionEnabled = motionEnabled,
            desktopPointerReady = desktopPointerReady,
            touchpadClickSuppressed = touchpadClickSuppressed,
            headTrackingActive = xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
        CompanionPointerButtonsRow(
            desktopPointerReady = desktopPointerReady,
            headTrackingActive = xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }

    if (showControls) {
        ModalBottomSheet(
            onDismissRequest = { showControls = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShowLauncherOnGlassesButton(
                        onClick = { DisplayLaunchHelper.showLauncherOnGlasses(context) },
                        modifier = Modifier.weight(1f),
                    )
                    AllAppsOnGlassesButton(
                        overlayVisible = allAppsOverlayVisible,
                        onClick = { DisplayLaunchHelper.toggleAllAppsOnGlasses(context) },
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
                                    },
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
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
                        allAppsOverlayVisible = allAppsOverlayVisible,
                        cursorHoveredLabel = cursor.hoveredLabel,
                        focusedPanelId = focusedPanelId,
                        launcherForeground = launcherForeground,
                    )
                    CompanionTab.Input -> InputTabContent(
                        xrInputMode = xrInputMode,
                        rayNeoUsbAttached = rayNeoUsbAttached,
                        headTrackingState = headTrackingState,
                        headTrackingError = headTrackingError,
                        motionAvailable = motionAvailable,
                        motionEnabled = motionEnabled,
                        isCalibrating = isCalibrating,
                        onCalibrate = onCalibrate,
                        onXrInputModeChange = { mode ->
                            GlassesSessionState.xrInputMode = mode
                            if (mode == GlassesXrInputMode.GLASSES_HEAD_TRACKING) {
                                GlassesSessionState.controlMode = GlassesControlMode.LAUNCHER
                                CompanionPointerBus.setGlassesControlMode(GlassesControlMode.LAUNCHER)
                                CompanionPointerBus.recenterCursor()
                                val appearance = workspace?.appearance?.clamped()
                                if (appearance != null) {
                                    scope.launch {
                                        workspaceRepository.updateAppearance(
                                            appearance.copy(
                                                lookYawDegrees = 0f,
                                                lookPitchDegrees = 0f,
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(stringResource(R.string.settings_open))
                }
            }
        }
    }
}

@Composable
private fun AllAppsOnGlassesButton(
    overlayVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Icon(
            imageVector = if (overlayVisible) Icons.Default.Close else Icons.Default.Apps,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = stringResource(
                if (overlayVisible) {
                    R.string.all_apps_close_on_glasses
                } else {
                    R.string.all_apps_on_glasses
                },
            ),
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
    allAppsOverlayVisible: Boolean,
    cursorHoveredLabel: String?,
    focusedPanelId: String?,
    launcherForeground: Boolean,
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
    if (allAppsOverlayVisible) {
        CompanionAllAppsPageControls(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }

    CompanionWorkspaceLiveControls(
        cursorHoveredLabel = cursorHoveredLabel,
        focusedPanelId = focusedPanelId,
        launcherForeground = launcherForeground,
    )
}

@Composable
private fun InputTabContent(
    xrInputMode: GlassesXrInputMode,
    rayNeoUsbAttached: Boolean,
    headTrackingState: RayNeoHeadTrackingState,
    headTrackingError: String?,
    motionAvailable: Boolean,
    motionEnabled: Boolean,
    isCalibrating: Boolean,
    onCalibrate: () -> Unit,
    onXrInputModeChange: (GlassesXrInputMode) -> Unit,
) {
    Text(
        text = stringResource(R.string.xr_input_mode_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = if (rayNeoUsbAttached) {
            stringResource(R.string.xr_input_mode_rayneo_detected)
        } else {
            stringResource(R.string.xr_input_mode_rayneo_missing)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    XrInputModeOption(
        selected = xrInputMode == GlassesXrInputMode.COMPANION,
        title = stringResource(R.string.xr_input_mode_companion),
        subtitle = stringResource(R.string.xr_input_mode_companion_hint),
        onSelect = { onXrInputModeChange(GlassesXrInputMode.COMPANION) },
    )
    XrInputModeOption(
        selected = xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING,
        title = stringResource(R.string.xr_input_mode_glasses_imu),
        subtitle = stringResource(R.string.xr_input_mode_glasses_imu_hint),
        enabled = rayNeoUsbAttached,
        onSelect = { onXrInputModeChange(GlassesXrInputMode.GLASSES_HEAD_TRACKING) },
    )

    if (xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING) {
        val statusText = when (headTrackingState) {
            RayNeoHeadTrackingState.STREAMING -> stringResource(R.string.xr_head_tracking_streaming)
            RayNeoHeadTrackingState.PERMISSION_REQUIRED ->
                stringResource(R.string.xr_head_tracking_permission)
            RayNeoHeadTrackingState.CONNECTING -> stringResource(R.string.xr_head_tracking_connecting)
            RayNeoHeadTrackingState.ERROR -> headTrackingError
                ?: stringResource(R.string.xr_head_tracking_error)
            RayNeoHeadTrackingState.DISCONNECTED -> stringResource(R.string.xr_head_tracking_disconnected)
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.settings_advanced_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedButton(
            onClick = { CompanionPointerBus.recenterHeadLook() },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.workspace_look_recenter_forward))
        }
        HeadTrackingCalibrationWizard(
            headTrackingState = headTrackingState,
            modifier = Modifier.padding(top = 12.dp),
        )
        return
    }

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
        OutlinedButton(
            onClick = { CompanionPointerBus.recenterCursor() },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(stringResource(R.string.recenter))
        }
    }

    Text(
        text = stringResource(R.string.settings_advanced_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun XrInputModeOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompanionWorkspaceLiveControls(
    cursorHoveredLabel: String?,
    focusedPanelId: String?,
    launcherForeground: Boolean,
) {
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
