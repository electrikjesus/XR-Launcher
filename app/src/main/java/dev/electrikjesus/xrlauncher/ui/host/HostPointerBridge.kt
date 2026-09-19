package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialog
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.HostSpaceZoom
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.ui.external.isHostScreenChromeAt
import kotlin.math.hypot

/**
 * Maps absolute mouse/touch on the host Home Space into [CompanionPointerBus].
 *
 * Minecraft-style layers:
 * - Absolute cursor always (screen HUD / Edit stay hittable; no center lock).
 * - FPS look via move deltas only while no modal is open and not dragging.
 * - Screen chrome + Edit/Settings modals → Compose clickables (do not consume).
 * - Desk / empty sphere → Hold-Left on the pointer bus.
 * - Right-click → [PointerButton.RIGHT]; scroll / pinch → sphere zoom via [onZoomSphere].
 */
@Composable
fun HostPointerBridge(
    modifier: Modifier = Modifier,
    onZoomSphere: (delta: Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var leftDown by remember { mutableStateOf(false) }
    var deskGrab by remember { mutableStateOf(false) }
    var pinching by remember { mutableStateOf(false) }
    var pinchDistance by remember { mutableFloatStateOf(0f) }
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
                        val nx = (change.position.x / w).coerceIn(0f, 1f)
                        val ny = (change.position.y / h).coerceIn(0f, 1f)
                        val fpsLook = GlassesLookMode.effective() == GlassesLookMode.FPS
                        val dialogOpen = GlassesSessionState.homeSpaceEdit ||
                            HomeSpaceDialogState.dialog != HomeSpaceDialog.NONE
                        val pressed = changes.filter { it.pressed }

                        when (event.type) {
                            PointerEventType.Scroll -> {
                                if (!dialogOpen) {
                                    val scrollY = change.scrollDelta.y
                                    val delta = HostSpaceZoom.sphereDeltaFromScroll(scrollY)
                                    if (delta != 0f) zoomLatest(delta)
                                    change.consume()
                                }
                            }
                            PointerEventType.Move, PointerEventType.Enter -> {
                                CompanionPointerBus.setCursorPosition(nx, ny)
                                if (pinching && pressed.size >= 2) {
                                    val dist = pinchDistanceOf(pressed[0].position, pressed[1].position)
                                    val delta = HostSpaceZoom.sphereDeltaFromPinch(pinchDistance, dist)
                                    if (delta != 0f) zoomLatest(delta)
                                    pinchDistance = dist
                                    pressed.forEach { it.consume() }
                                } else if (fpsLook && !leftDown && !dialogOpen && !pinching) {
                                    val delta = change.positionChange()
                                    if (delta.x != 0f || delta.y != 0f) {
                                        GlassesHomeLook.panNorm += HomeSpaceScene.fpsPanNormDelta(
                                            deltaX = delta.x / w,
                                            viewportWidthPx = w,
                                            viewportHeightPx = h,
                                        )
                                        GlassesHomeLook.lookPitch +=
                                            HomeSpaceScene.fpsPitchDelta(delta.y / h)
                                    }
                                }
                                if (deskGrab) {
                                    CompanionPointerBus.onPointerMoveWhilePressed?.invoke()
                                }
                            }
                            PointerEventType.Press -> {
                                CompanionPointerBus.setCursorPosition(nx, ny)
                                when {
                                    event.buttons.isSecondaryPressed -> {
                                        if (!dialogOpen && !isHostScreenChromeAt(nx, ny)) {
                                            CompanionPointerBus.click(PointerButton.RIGHT)
                                        }
                                    }
                                    pressed.size >= 2 -> {
                                        if (deskGrab) {
                                            CompanionPointerBus.endLeftButton()
                                            deskGrab = false
                                        }
                                        leftDown = false
                                        pinching = !dialogOpen
                                        if (pinching) {
                                            pinchDistance = pinchDistanceOf(
                                                pressed[0].position,
                                                pressed[1].position,
                                            )
                                        }
                                    }
                                    event.buttons.isPrimaryPressed || pressed.size == 1 -> {
                                        leftDown = true
                                        pinching = false
                                        val chrome = isHostScreenChromeAt(nx, ny)
                                        deskGrab = !dialogOpen && !chrome
                                        if (deskGrab) {
                                            CompanionPointerBus.beginLeftButton()
                                        }
                                    }
                                }
                            }
                            PointerEventType.Release -> {
                                CompanionPointerBus.setCursorPosition(nx, ny)
                                val stillPressed = changes.count { it.pressed }
                                if (pinching) {
                                    if (stillPressed < 2) {
                                        pinching = false
                                        pinchDistance = 0f
                                    }
                                } else if (leftDown && stillPressed == 0) {
                                    if (deskGrab) {
                                        CompanionPointerBus.endLeftButton()
                                    }
                                    leftDown = false
                                    deskGrab = false
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

private fun pinchDistanceOf(a: Offset, b: Offset): Float =
    hypot(a.x - b.x, a.y - b.y)
