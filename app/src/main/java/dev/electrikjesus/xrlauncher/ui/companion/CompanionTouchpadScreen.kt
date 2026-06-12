package dev.electrikjesus.xrlauncher.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.provider.Settings
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.core.display.GlassesControlMode
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.CursorStyles
import dev.electrikjesus.xrlauncher.core.input.PointerAction
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.input.PointerEvent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionTouchpadScreen(
    motionAvailable: Boolean = true,
    isCalibrating: Boolean = false,
    onCalibrate: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val motionEnabled by CompanionPointerBus.motionControlEnabled.collectAsState()
    val motionSensitivity by CompanionPointerBus.motionSensitivity.collectAsState()
    val controlMode by CompanionPointerBus.glassesControlMode.collectAsState()
    val context = LocalContext.current
    val desktopPointerReady = DisplayPointerInjector.isAvailable
    val cursorStyle = CursorStyles.forControlMode(controlMode)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.companion_touchpad)) })
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.control_mode_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = controlMode == GlassesControlMode.LAUNCHER,
                    onClick = { CompanionPointerBus.setGlassesControlMode(GlassesControlMode.LAUNCHER) },
                    label = { Text(stringResource(R.string.control_mode_launcher)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = controlMode == GlassesControlMode.DESKTOP,
                    onClick = { CompanionPointerBus.setGlassesControlMode(GlassesControlMode.DESKTOP) },
                    label = { Text(stringResource(R.string.control_mode_desktop)) },
                    enabled = desktopPointerReady,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (desktopPointerReady) {
                    if (controlMode == GlassesControlMode.DESKTOP) {
                        stringResource(R.string.control_mode_desktop_on_hint)
                    } else {
                        stringResource(R.string.control_mode_launcher_on_hint)
                    }
                } else {
                    stringResource(R.string.control_mode_desktop_setup_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!desktopPointerReady) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.enable_desktop_pointer))
                }
            }
            OutlinedButton(
                onClick = { DisplayLaunchHelper.showLauncherOnGlasses(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.show_launcher_on_glasses))
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
                    ) {
                        Text(stringResource(R.string.recenter))
                    }
                    OutlinedButton(
                        onClick = onCalibrate,
                        enabled = !isCalibrating,
                        modifier = Modifier.weight(1f),
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
            }

            if (cursor.hoveredLabel != null) {
                Text(
                    text = stringResource(R.string.cursor_over, cursor.hoveredLabel!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(motionEnabled, controlMode) {
                        val desktopDrag = controlMode == GlassesControlMode.DESKTOP
                        var lastTapTime = 0L
                        var lastTapPos = Offset.Zero
                        val doubleTapTimeoutMs = 300L
                        val doubleTapMinTimeMs = 40L
                        val doubleTapSlop = viewConfiguration.touchSlop * 2f

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var accumulated = Offset.Zero
                            val touchSlop = viewConfiguration.touchSlop
                            var dragging = false
                            val pointerId = down.id
                            val tapToClick = controlMode == GlassesControlMode.LAUNCHER
                            var clickDragActive = false

                            if (desktopDrag) {
                                val now = System.currentTimeMillis()
                                val isDoubleTap = lastTapTime > 0L &&
                                    now - lastTapTime in doubleTapMinTimeMs..doubleTapTimeoutMs &&
                                    (down.position - lastTapPos).getDistance() <= doubleTapSlop
                                if (isDoubleTap) {
                                    lastTapTime = 0L
                                    CompanionPointerBus.beginTouchpadDragGesture()
                                    clickDragActive = true
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    when {
                                        desktopDrag -> {
                                            if (clickDragActive) {
                                                CompanionPointerBus.endTouchpadDragGesture()
                                            } else if (!dragging) {
                                                lastTapTime = System.currentTimeMillis()
                                                lastTapPos = down.position
                                            }
                                        }
                                        tapToClick && !dragging -> CompanionPointerBus.click(PointerButton.LEFT)
                                    }
                                    break
                                }
                                val delta = change.positionChange()
                                if (!dragging) {
                                    accumulated += delta
                                    if (accumulated.getDistance() > touchSlop) {
                                        dragging = true
                                    }
                                }
                                if (dragging && !motionEnabled) {
                                    change.consume()
                                    CompanionPointerBus.emit(
                                        PointerEvent(
                                            action = PointerAction.MOVE,
                                            deltaX = delta.x,
                                            deltaY = delta.y,
                                        ),
                                    )
                                }
                            }
                        }
                    },
            ) {
                val density = LocalDensity.current
                val touchpadWidthPx = with(density) { maxWidth.toPx() }
                val touchpadHeightPx = with(density) { maxHeight.toPx() }
                val cursorXPx = cursor.x * touchpadWidthPx
                val cursorYPx = cursor.y * touchpadHeightPx
                val halfPx = with(density) { cursorStyle.halfDotSize.toPx() }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = when {
                            motionEnabled -> stringResource(R.string.companion_motion_hint)
                            controlMode == GlassesControlMode.DESKTOP ->
                                stringResource(R.string.companion_desktop_hint)
                            else -> stringResource(R.string.companion_hint)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.cursor_position,
                            (cursor.x * 100).toInt(),
                            (cursor.y * 100).toInt(),
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (cursorXPx - halfPx).roundToInt(),
                                (cursorYPx - halfPx).roundToInt(),
                            )
                        }
                        .size(cursorStyle.dotSize)
                        .clip(CircleShape)
                        .alpha(cursorStyle.dotAlpha)
                        .background(
                            if (cursor.isPressed) Color(0xFF6750A4) else Color(0xFF03DAC5),
                        ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val useHoldLeft = desktopPointerReady && controlMode == GlassesControlMode.DESKTOP
                if (useHoldLeft) {
                    HoldablePointerButton(
                        label = stringResource(R.string.left_click),
                        modifier = Modifier.weight(1f),
                        onPress = { CompanionPointerBus.beginLeftButton() },
                        onRelease = { CompanionPointerBus.endLeftButton() },
                    )
                } else {
                    Button(
                        onClick = { CompanionPointerBus.click(PointerButton.LEFT) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.left_click))
                    }
                }
                OutlinedButton(
                    onClick = { CompanionPointerBus.click(PointerButton.RIGHT) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.right_click))
                }
            }
        }
    }
}

@Composable
private fun HoldablePointerButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onPress()
                    val pointerId = down.id
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                        }
                    } finally {
                        onRelease()
                    }
                }
            }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
