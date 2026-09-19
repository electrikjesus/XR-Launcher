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
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene

/**
 * Maps absolute mouse/touch on the host Home Space surface into [CompanionPointerBus]
 * so desk / HUD work without a phone companion.
 */
@Composable
fun HostPointerBridge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var leftDown by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        val w = size.width.coerceAtLeast(1).toFloat()
                        val h = size.height.coerceAtLeast(1).toFloat()
                        val nx = (change.position.x / w).coerceIn(0f, 1f)
                        val ny = (change.position.y / h).coerceIn(0f, 1f)
                        val lookMode = GlassesLookMode.effective()
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                if (lookMode == GlassesLookMode.FPS && !leftDown) {
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
                                    CompanionPointerBus.setCursorPosition(0.5f, 0.5f)
                                } else {
                                    CompanionPointerBus.setCursorPosition(nx, ny)
                                    if (leftDown) {
                                        CompanionPointerBus.onPointerMoveWhilePressed?.invoke()
                                    }
                                }
                            }
                            PointerEventType.Press -> {
                                CompanionPointerBus.setCursorPosition(
                                    if (lookMode == GlassesLookMode.FPS) 0.5f else nx,
                                    if (lookMode == GlassesLookMode.FPS) 0.5f else ny,
                                )
                                if (!leftDown) {
                                    leftDown = true
                                    CompanionPointerBus.beginLeftButton()
                                }
                                change.consume()
                            }
                            PointerEventType.Release -> {
                                if (leftDown) {
                                    leftDown = false
                                    CompanionPointerBus.endLeftButton()
                                    change.consume()
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
