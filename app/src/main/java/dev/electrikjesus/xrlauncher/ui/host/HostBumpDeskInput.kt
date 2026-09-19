package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.input.bumpdesk.BumpDeskHostAction
import dev.electrikjesus.xrlauncher.core.input.bumpdesk.BumpDeskHostGesture
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialog
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.HostSpaceZoom
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.ui.external.isHostScreenChromeAt
import kotlin.math.hypot

/**
 * BumpDesk-derived host pointer for Expanded Home Space.
 *
 * Absolute screen coords (no companion FPS press/release re-lock), touch-slop before desk
 * Hold-Left, middle-button drag → look pan, scroll/pinch → sphere zoom, secondary → right-click.
 * Screen-locked HUD / Edit-Settings modals stay on Compose clickables.
 */
@Composable
fun HostBumpDeskInput(
    modifier: Modifier = Modifier,
    onZoomSphere: (delta: Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val gesture = remember { BumpDeskHostGesture() }
    val zoomLatest by rememberUpdatedState(onZoomSphere)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val changes = event.changes
                        val change = changes.firstOrNull() ?: continue
                        val w = size.width.coerceAtLeast(1).toFloat()
                        val h = size.height.coerceAtLeast(1).toFloat()
                        val x = change.position.x
                        val y = change.position.y
                        val nx = (x / w).coerceIn(0f, 1f)
                        val ny = (y / h).coerceIn(0f, 1f)
                        val fpsLook = GlassesLookMode.effective() == GlassesLookMode.FPS
                        val dialogOpen = GlassesSessionState.homeSpaceEdit ||
                            HomeSpaceDialogState.dialog != HomeSpaceDialog.NONE
                        val pressed = changes.filter { it.pressed }
                        val chrome = isHostScreenChromeAt(nx, ny)
                        val allowDesk = !dialogOpen && !chrome

                        fun apply(actions: List<BumpDeskHostAction>) {
                            for (action in actions) {
                                applyAction(action, w, h, zoomLatest)
                            }
                        }

                        when (event.type) {
                            PointerEventType.Scroll -> {
                                if (!dialogOpen) {
                                    val delta = HostSpaceZoom.sphereDeltaFromScroll(change.scrollDelta.y)
                                    if (delta != 0f) zoomLatest(delta)
                                    change.consume()
                                }
                            }
                            PointerEventType.Move, PointerEventType.Enter -> {
                                when {
                                    gesture.pinching && pressed.size >= 2 -> {
                                        val dist = pinchDistanceOf(pressed[0].position, pressed[1].position)
                                        val mid = pinchMidpoint(pressed[0].position, pressed[1].position)
                                        apply(gesture.onPinchMove(dist, mid.x, mid.y))
                                        pressed.forEach { it.consume() }
                                    }
                                    else -> apply(
                                        gesture.onMove(
                                            x = x,
                                            y = y,
                                            allowDeskGrab = allowDesk,
                                            fpsLook = fpsLook,
                                            dialogOpen = dialogOpen,
                                        ),
                                    )
                                }
                            }
                            PointerEventType.Press -> {
                                when {
                                    event.buttons.isTertiaryPressed -> {
                                        apply(listOf(gesture.onMiddleDown(x, y)))
                                    }
                                    event.buttons.isSecondaryPressed -> {
                                        if (allowDesk) {
                                            apply(listOf(gesture.onSecondaryDown(x, y)))
                                        } else {
                                            CompanionPointerBus.setCursorPosition(nx, ny)
                                        }
                                    }
                                    pressed.size >= 2 -> {
                                        if (gesture.deskDragArmed) {
                                            CompanionPointerBus.endLeftButton()
                                        }
                                        val dist = pinchDistanceOf(pressed[0].position, pressed[1].position)
                                        val mid = pinchMidpoint(pressed[0].position, pressed[1].position)
                                        apply(listOf(gesture.onPinchBegin(dist, mid.x, mid.y)))
                                    }
                                    event.buttons.isPrimaryPressed || pressed.size == 1 -> {
                                        apply(gesture.onPrimaryDown(x, y, allowDeskGrab = allowDesk))
                                    }
                                }
                            }
                            PointerEventType.Release -> {
                                val stillPressed = changes.count { it.pressed }
                                when {
                                    gesture.pinching -> {
                                        if (stillPressed < 2) {
                                            apply(listOf(gesture.onPinchEnd()))
                                        }
                                    }
                                    gesture.middleDragging -> {
                                        if (stillPressed == 0) {
                                            apply(listOf(gesture.onMiddleUp(x, y)))
                                        }
                                    }
                                    gesture.primaryDown && stillPressed == 0 -> {
                                        apply(gesture.onPrimaryUp(x, y))
                                    }
                                    else -> CompanionPointerBus.setCursorPosition(nx, ny)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            },
    ) {
        content()
    }
}

private fun applyAction(
    action: BumpDeskHostAction,
    viewportW: Float,
    viewportH: Float,
    onZoom: (Float) -> Unit,
) {
    when (action) {
        BumpDeskHostAction.None -> Unit
        is BumpDeskHostAction.CursorAt -> {
            val nx = (action.x / viewportW).coerceIn(0f, 1f)
            val ny = (action.y / viewportH).coerceIn(0f, 1f)
            CompanionPointerBus.setCursorPosition(nx, ny)
        }
        BumpDeskHostAction.BeginDeskHold -> CompanionPointerBus.beginLeftButton()
        BumpDeskHostAction.EndDeskHold -> CompanionPointerBus.endLeftButton()
        BumpDeskHostAction.DeskMoveWhilePressed ->
            CompanionPointerBus.onPointerMoveWhilePressed?.invoke()
        BumpDeskHostAction.CancelDeskHold -> {
            if (CompanionPointerBus.cursor.value.isPressed) {
                CompanionPointerBus.endLeftButton()
            }
        }
        is BumpDeskHostAction.LeftClick -> {
            val nx = (action.x / viewportW).coerceIn(0f, 1f)
            val ny = (action.y / viewportH).coerceIn(0f, 1f)
            CompanionPointerBus.setCursorPosition(nx, ny)
            CompanionPointerBus.click(PointerButton.LEFT)
        }
        is BumpDeskHostAction.RightClick -> {
            val nx = (action.x / viewportW).coerceIn(0f, 1f)
            val ny = (action.y / viewportH).coerceIn(0f, 1f)
            CompanionPointerBus.setCursorPosition(nx, ny)
            CompanionPointerBus.click(PointerButton.RIGHT)
        }
        is BumpDeskHostAction.LookPan -> {
            GlassesHomeLook.panNorm += HomeSpaceScene.fpsPanNormDelta(
                deltaX = action.dxPx / viewportW,
                viewportWidthPx = viewportW,
                viewportHeightPx = viewportH,
            )
            GlassesHomeLook.lookPitch += HomeSpaceScene.fpsPitchDelta(action.dyPx / viewportH)
        }
        is BumpDeskHostAction.PinchZoom -> {
            val delta = HostSpaceZoom.sphereDeltaFromPinch(action.previousDistance, action.currentDistance)
            if (delta != 0f) onZoom(delta)
        }
    }
}

private fun pinchDistanceOf(a: Offset, b: Offset): Float =
    hypot(a.x - b.x, a.y - b.y)

private fun pinchMidpoint(a: Offset, b: Offset): Offset =
    Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
