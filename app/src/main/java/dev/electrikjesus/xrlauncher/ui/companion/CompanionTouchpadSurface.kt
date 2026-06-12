package dev.electrikjesus.xrlauncher.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.CursorStyles
import dev.electrikjesus.xrlauncher.core.input.PointerAction
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.input.PointerEvent
import kotlin.math.roundToInt

@Composable
fun CompanionTouchpadSurface(
    motionEnabled: Boolean,
    desktopPointerReady: Boolean,
    touchpadClickSuppressed: Boolean,
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val cursorStyle = CursorStyles.forPointerReady(desktopPointerReady)

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        ),
                    ),
                )
                .pointerInput(motionEnabled, desktopPointerReady, touchpadClickSuppressed) {
                    val useDesktopGestures = desktopPointerReady
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
                        val tapToClick = !useDesktopGestures

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                when {
                                    useDesktopGestures -> {
                                        if (!dragging && !touchpadClickSuppressed) {
                                            val now = System.currentTimeMillis()
                                            val isSecondTap = lastTapTime > 0L &&
                                                now - lastTapTime in doubleTapMinTimeMs..doubleTapTimeoutMs &&
                                                (down.position - lastTapPos).getDistance() <= doubleTapSlop
                                            if (isSecondTap) {
                                                lastTapTime = 0L
                                                CompanionPointerBus.click(PointerButton.LEFT)
                                            } else {
                                                lastTapTime = now
                                                lastTapPos = down.position
                                            }
                                        } else if (dragging) {
                                            lastTapTime = 0L
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
                                    lastTapTime = 0L
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
            Text(
                text = stringResource(R.string.companion_touchpad_zone),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when {
                        motionEnabled -> stringResource(R.string.companion_motion_hint)
                        desktopPointerReady -> stringResource(R.string.companion_desktop_hint)
                        else -> stringResource(R.string.companion_hint)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.cursor_position,
                        (cursor.x * 100).toInt(),
                        (cursor.y * 100).toInt(),
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val density = LocalDensity.current
            val touchpadWidthPx = with(density) { maxWidth.toPx() }
            val touchpadHeightPx = with(density) { maxHeight.toPx() }
            val cursorXPx = cursor.x * touchpadWidthPx
            val cursorYPx = cursor.y * touchpadHeightPx
            val halfPx = with(density) { cursorStyle.halfDotSize.toPx() }

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
                        if (cursor.isPressed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    ),
            )
        }
    }
}

@Composable
fun CompanionPointerButtonsRow(
    desktopPointerReady: Boolean,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        val useHoldLeft = desktopPointerReady
        if (useHoldLeft) {
            HoldablePointerButton(
                label = stringResource(R.string.left_click),
                modifier = Modifier.weight(1f),
                onPress = { CompanionPointerBus.beginLeftButton() },
                onRelease = { CompanionPointerBus.endLeftButton() },
            )
        } else {
            androidx.compose.material3.Button(
                onClick = { CompanionPointerBus.click(PointerButton.LEFT) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.left_click))
            }
        }
        androidx.compose.material3.FilledTonalButton(
            onClick = { CompanionPointerBus.click(PointerButton.RIGHT) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.right_click))
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
            .clip(MaterialTheme.shapes.large)
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
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
