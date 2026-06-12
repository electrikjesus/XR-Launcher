package dev.electrikjesus.xrlauncher.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerAction
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.input.PointerEvent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionTouchpadScreen(
    motionAvailable: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val motionEnabled by CompanionPointerBus.motionControlEnabled.collectAsState()

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
                    .pointerInput(motionEnabled) {
                        if (!motionEnabled) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    CompanionPointerBus.setCursorPosition(
                                        x = offset.x / size.width,
                                        y = offset.y / size.height,
                                    )
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    CompanionPointerBus.emit(
                                        PointerEvent(
                                            action = PointerAction.MOVE,
                                            deltaX = dragAmount.x,
                                            deltaY = dragAmount.y,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            CompanionPointerBus.setCursorPosition(
                                x = offset.x / size.width,
                                y = offset.y / size.height,
                            )
                            CompanionPointerBus.click(PointerButton.LEFT)
                        }
                    },
            ) {
                val density = LocalDensity.current
                val touchpadWidthPx = with(density) { maxWidth.toPx() }
                val touchpadHeightPx = with(density) { maxHeight.toPx() }
                val cursorXPx = cursor.x * touchpadWidthPx
                val cursorYPx = cursor.y * touchpadHeightPx

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (motionEnabled) {
                            stringResource(R.string.companion_motion_hint)
                        } else {
                            stringResource(R.string.companion_hint)
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
                                (cursorXPx - with(density) { 14.dp.toPx() }).roundToInt(),
                                (cursorYPx - with(density) { 14.dp.toPx() }).roundToInt(),
                            )
                        }
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (cursor.isPressed) Color(0xFF6750A4) else Color(0xFF03DAC5),
                        ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { CompanionPointerBus.click(PointerButton.LEFT) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.left_click))
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
