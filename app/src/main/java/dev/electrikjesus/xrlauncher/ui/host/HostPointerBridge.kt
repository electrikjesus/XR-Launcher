package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialog
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.ui.external.isHostScreenChromeAt

/**
 * Maps absolute mouse/touch on the host Home Space into [CompanionPointerBus].
 *
 * Minecraft-style layers:
 * - Absolute cursor always (screen HUD / Edit stay hittable; no center lock).
 * - FPS look via move deltas only while no modal is open and not dragging.
 * - Screen chrome + Edit/Settings modals → Compose clickables (do not consume).
 * - Desk / empty sphere → Hold-Left on the pointer bus.
 */
@Composable
fun HostPointerBridge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var leftDown by remember { mutableStateOf(false) }
    var deskGrab by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        val w = size.width.coerceAtLeast(1).toFloat()
                        val h = size.height.coerceAtLeast(1).toFloat()
                        val nx = (change.position.x / w).coerceIn(0f, 1f)
                        val ny = (change.position.y / h).coerceIn(0f, 1f)
                        val fpsLook = GlassesLookMode.effective() == GlassesLookMode.FPS
                        val dialogOpen = GlassesSessionState.homeSpaceEdit ||
                            HomeSpaceDialogState.dialog != HomeSpaceDialog.NONE

                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                CompanionPointerBus.setCursorPosition(nx, ny)
                                if (fpsLook && !leftDown && !dialogOpen) {
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
                                leftDown = true
                                val chrome = isHostScreenChromeAt(nx, ny)
                                deskGrab = !dialogOpen && !chrome
                                if (deskGrab) {
                                    CompanionPointerBus.beginLeftButton()
                                }
                                // Do not consume — HUD / Edit / Settings Compose clickables need the event.
                            }
                            PointerEventType.Release -> {
                                CompanionPointerBus.setCursorPosition(nx, ny)
                                if (leftDown) {
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
