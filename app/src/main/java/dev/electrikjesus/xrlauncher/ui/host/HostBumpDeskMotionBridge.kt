package dev.electrikjesus.xrlauncher.ui.host

import android.view.MotionEvent
import android.util.Log
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.input.bumpdesk.BumpDeskHostAction
import dev.electrikjesus.xrlauncher.core.input.bumpdesk.BumpDeskHostGesture
import dev.electrikjesus.xrlauncher.core.workspace.DeskLassoState
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDeskState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialog
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.HostSpaceZoom
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.ui.external.isHostScreenChromeAt
import kotlin.math.hypot

/**
 * GLSurfaceView eats MotionEvents outside Compose's pointerInput. Host FPS look / desk
 * gestures must be fed from [android.opengl.GLSurfaceView.setOnTouchListener] while the
 * screen-locked HUD stays on Compose clickables above the surface.
 */
object HostBumpDeskMotionBridge {
    private val gesture = BumpDeskHostGesture()
    @Volatile
    private var onZoomSphere: (Float) -> Unit = {}

    fun bind(onZoom: (Float) -> Unit) {
        onZoomSphere = onZoom
    }

    fun reset() {
        gesture.reset()
        onZoomSphere = {}
    }

    /** @return true if the event was handled (surface should consume). */
    fun onTouch(event: MotionEvent, viewportW: Int, viewportH: Int): Boolean {
        if (!GlassesSessionState.hostImmersiveSession) return false
        val w = viewportW.coerceAtLeast(1).toFloat()
        val h = viewportH.coerceAtLeast(1).toFloat()
        val x = event.x
        val y = event.y
        val nx = (x / w).coerceIn(0f, 1f)
        val ny = (y / h).coerceIn(0f, 1f)
        val chrome = isHostScreenChromeAt(nx, ny)
        // Let Compose own the top HUD / Edit strip.
        if (chrome) return false

        val fpsLook = GlassesLookMode.effective() == GlassesLookMode.FPS
        val gestureLook = GlassesLookMode.effective() == GlassesLookMode.GESTURE
        val dialogOpen = GlassesSessionState.homeSpaceEdit ||
            HomeSpaceDialogState.dialog != HomeSpaceDialog.NONE
        // Modal Settings/Edit: let Compose own the event (catcher is also removed while open).
        if (dialogOpen) return false
        val allowDesk = true

        fun apply(actions: List<BumpDeskHostAction>) {
            for (action in actions) {
                applyAction(action, w, h, onZoomSphere)
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount >= 2) {
                    val dist = pinchDistance(event)
                    val mid = pinchMid(event)
                    if (gesture.deskDragArmed) CompanionPointerBus.endLeftButton()
                    apply(listOf(gesture.onPinchBegin(dist, mid.first, mid.second)))
                } else {
                    apply(
                        gesture.onPrimaryDown(
                            x = x,
                            y = y,
                            allowDeskGrab = allowDesk,
                            fpsLook = fpsLook,
                        ),
                    )
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    if (gesture.deskDragArmed) CompanionPointerBus.endLeftButton()
                    val dist = pinchDistance(event)
                    val mid = pinchMid(event)
                    apply(listOf(gesture.onPinchBegin(dist, mid.first, mid.second)))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when {
                    gesture.pinching && event.pointerCount >= 2 -> {
                        val dist = pinchDistance(event)
                        val mid = pinchMid(event)
                        apply(gesture.onPinchMove(dist, mid.first, mid.second))
                    }
                    gesture.middleDragging ->
                        apply(
                            gesture.onMove(
                                x = x,
                                y = y,
                                allowDeskGrab = allowDesk,
                                fpsLook = fpsLook,
                                dialogOpen = dialogOpen,
                                gestureLook = gestureLook,
                            ),
                        )
                    else ->
                        apply(
                            gesture.onMove(
                                x = x,
                                y = y,
                                allowDeskGrab = allowDesk,
                                fpsLook = fpsLook,
                                dialogOpen = dialogOpen,
                                gestureLook = gestureLook,
                            ),
                        )
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (gesture.pinching && event.pointerCount < 2) {
                    apply(listOf(gesture.onPinchEnd()))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                when {
                    gesture.pinching -> apply(listOf(gesture.onPinchEnd()))
                    gesture.middleDragging -> apply(listOf(gesture.onMiddleUp(x, y)))
                    gesture.primaryDown -> apply(gesture.onPrimaryUp(x, y))
                    else -> CompanionPointerBus.setCursorPosition(nx, ny)
                }
            }
            // OS mouse without button — classic FPS mouse-look.
            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                if (!fpsLook || dialogOpen) {
                    CompanionPointerBus.setCursorPosition(nx, ny)
                    return true
                }
                apply(
                    gesture.onMove(
                        x = x,
                        y = y,
                        allowDeskGrab = allowDesk,
                        fpsLook = true,
                        dialogOpen = false,
                    ),
                )
            }
            else -> return false
        }
        return true
    }

    private fun pinchDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return hypot(
            event.getX(0) - event.getX(1),
            event.getY(0) - event.getY(1),
        )
    }

    private fun pinchMid(event: MotionEvent): Pair<Float, Float> {
        if (event.pointerCount < 2) return event.x to event.y
        return (event.getX(0) + event.getX(1)) * 0.5f to
            (event.getY(0) + event.getY(1)) * 0.5f
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
                val grabbing = HomeSpaceDeskState.hasActiveGesture() || DeskLassoState.active
                if (GlassesLookMode.effective() == GlassesLookMode.GESTURE && grabbing) {
                    Unit
                } else {
                    GlassesHomeLook.addLookDegrees(
                        yawDeltaDeg = HomeSpaceScene.fpsYawDegreesDelta(
                            deltaXNorm = action.dxPx / viewportW,
                            viewportWidthPx = viewportW,
                            viewportHeightPx = viewportH,
                        ),
                        pitchDeltaDeg = HomeSpaceScene.fpsPitchDelta(action.dyPx / viewportH),
                    )
                }
            }
            is BumpDeskHostAction.PinchZoom -> {
                val delta = HostSpaceZoom.sphereDeltaFromPinch(
                    action.previousDistance,
                    action.currentDistance,
                )
                if (delta != 0f) onZoom(delta)
            }
        }
    }

    /**
     * Compose catcher under the top HUD — GLSurfaceView interop never reaches parent
     * pointerInput, so desk / FPS look must be driven from this path on host.
     */
    fun onComposePointer(
        actionMasked: Int,
        x: Float,
        y: Float,
        viewportW: Float,
        viewportH: Float,
        pointerCount: Int = 1,
        secondX: Float = x,
        secondY: Float = y,
    ): Boolean {
        if (!GlassesSessionState.hostImmersiveSession) return false
        val nx = (x / viewportW.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val ny = (y / viewportH.coerceAtLeast(1f)).coerceIn(0f, 1f)
        if (isHostScreenChromeAt(nx, ny)) return false

        val fpsLook = GlassesLookMode.effective() == GlassesLookMode.FPS
        val gestureLook = GlassesLookMode.effective() == GlassesLookMode.GESTURE
        val dialogOpen = GlassesSessionState.homeSpaceEdit ||
            HomeSpaceDialogState.dialog != HomeSpaceDialog.NONE
        // Modal Settings/Edit: let Compose own the event (catcher is also removed while open).
        if (dialogOpen) return false
        val allowDesk = true

        fun apply(actions: List<BumpDeskHostAction>) {
            for (action in actions) {
                applyAction(action, viewportW, viewportH, onZoomSphere)
            }
        }

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (pointerCount >= 2) {
                    val dist = hypot(x - secondX, y - secondY)
                    if (gesture.deskDragArmed) CompanionPointerBus.endLeftButton()
                    apply(
                        listOf(
                            gesture.onPinchBegin(
                                dist,
                                (x + secondX) * 0.5f,
                                (y + secondY) * 0.5f,
                            ),
                        ),
                    )
                } else {
                    apply(gesture.onPrimaryDown(x, y, allowDesk, fpsLook))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (gesture.pinching && pointerCount >= 2) {
                    val dist = hypot(x - secondX, y - secondY)
                    apply(
                        gesture.onPinchMove(
                            dist,
                            (x + secondX) * 0.5f,
                            (y + secondY) * 0.5f,
                        ),
                    )
                } else {
                    apply(gesture.onMove(x, y, allowDesk, fpsLook, dialogOpen, gestureLook))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                when {
                    gesture.pinching -> apply(listOf(gesture.onPinchEnd()))
                    gesture.middleDragging -> apply(listOf(gesture.onMiddleUp(x, y)))
                    gesture.primaryDown -> apply(gesture.onPrimaryUp(x, y))
                    else -> CompanionPointerBus.setCursorPosition(nx, ny)
                }
            }
            else -> return false
        }
        return true
    }
}
