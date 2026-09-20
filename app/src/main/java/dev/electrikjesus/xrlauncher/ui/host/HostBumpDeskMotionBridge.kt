package dev.electrikjesus.xrlauncher.ui.host

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
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
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceScene
import dev.electrikjesus.xrlauncher.ui.external.abortDeskPointerGesture
import dev.electrikjesus.xrlauncher.ui.external.isHostScreenChromeAt
import kotlin.math.hypot

/**
 * GLSurfaceView eats MotionEvents outside Compose's pointerInput. Host FPS look / desk
 * gestures must be fed from [android.opengl.GLSurfaceView.setOnTouchListener] while the
 * screen-locked HUD stays on Compose clickables above the surface.
 */
object HostBumpDeskMotionBridge {
    private val gesture = BumpDeskHostGesture()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var lastViewportW: Float = 1f
    private var lastViewportH: Float = 1f
    @Volatile
    private var onZoomSphere: (Float) -> Unit = {}

    fun bind(onZoom: (Float) -> Unit) {
        onZoomSphere = onZoom
    }

    fun reset() {
        cancelLongPress()
        gesture.reset()
        onZoomSphere = {}
    }

    /** @return true if the event was handled (surface should consume). */
    fun onTouch(event: MotionEvent, viewportW: Int, viewportH: Int): Boolean {
        if (!GlassesSessionState.hostImmersiveSession) return false
        // Radial / context menu owns the pointer (catcher is also removed while open).
        if (LauncherContextMenuState.isOpen) return false
        val w = viewportW.coerceAtLeast(1).toFloat()
        val h = viewportH.coerceAtLeast(1).toFloat()
        lastViewportW = w
        lastViewportH = h
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

        val secondary = (event.buttonState and MotionEvent.BUTTON_SECONDARY) != 0
        val tertiary = (event.buttonState and MotionEvent.BUTTON_TERTIARY) != 0

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                when {
                    tertiary -> {
                        cancelLongPress()
                        apply(listOf(gesture.onMiddleDown(x, y)))
                    }
                    secondary -> {
                        cancelLongPress()
                        apply(listOf(gesture.onSecondaryDown(x, y)))
                    }
                    event.pointerCount >= 2 -> {
                        cancelLongPress()
                        val dist = pinchDistance(event)
                        val mid = pinchMid(event)
                        apply(listOf(gesture.onPinchBegin(dist, mid.first, mid.second)))
                    }
                    else -> {
                        apply(
                            gesture.onPrimaryDown(
                                x = x,
                                y = y,
                                allowDeskGrab = allowDesk,
                                fpsLook = fpsLook,
                            ),
                        )
                        if (allowDesk && !fpsLook) scheduleLongPress()
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                cancelLongPress()
                if (event.pointerCount >= 2) {
                    val dist = pinchDistance(event)
                    val mid = pinchMid(event)
                    apply(listOf(gesture.onPinchBegin(dist, mid.first, mid.second)))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when {
                    event.pointerCount >= 2 -> {
                        cancelLongPress()
                        val dist = pinchDistance(event)
                        val mid = pinchMid(event)
                        if (!gesture.pinching) {
                            apply(listOf(gesture.onPinchBegin(dist, mid.first, mid.second)))
                        }
                        apply(gesture.onPinchMove(dist, mid.first, mid.second, gestureLook = gestureLook))
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
                    else -> {
                        val actions = gesture.onMove(
                            x = x,
                            y = y,
                            allowDeskGrab = allowDesk,
                            fpsLook = fpsLook,
                            dialogOpen = dialogOpen,
                            gestureLook = gestureLook,
                        )
                        if (actions.any { it === BumpDeskHostAction.DeskMoveWhilePressed }) {
                            cancelLongPress()
                        }
                        apply(actions)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (gesture.pinching && event.pointerCount < 2) {
                    apply(listOf(gesture.onPinchEnd()))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
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

    private fun scheduleLongPress() {
        cancelLongPress()
        val timeout = ViewConfiguration.getLongPressTimeout().toLong()
        val runnable = Runnable {
            longPressRunnable = null
            gesture.onLongPressEmpty()?.let { action ->
                applyAction(action, lastViewportW, lastViewportH, onZoomSphere)
            }
        }
        longPressRunnable = runnable
        mainHandler.postDelayed(runnable, timeout)
    }

    private fun cancelLongPress() {
        longPressRunnable?.let { mainHandler.removeCallbacks(it) }
        longPressRunnable = null
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
                // Second finger / pinch: abandon one-finger desk work without selecting.
                abortDeskPointerGesture()
                CompanionPointerBus.abortLeftButton()
            }
            is BumpDeskHostAction.LongPressEmpty -> {
                // BumpDesk GestureDetector.onLongPress → radial; abort pending lasso / hold.
                cancelLongPress()
                val nx = (action.x / viewportW).coerceIn(0f, 1f)
                val ny = (action.y / viewportH).coerceIn(0f, 1f)
                CompanionPointerBus.setCursorPosition(nx, ny)
                abortDeskPointerGesture()
                CompanionPointerBus.abortLeftButton()
                CompanionPointerBus.click(PointerButton.RIGHT)
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
                // While pinching, always allow look — do not let a stale lasso block pan.
                val grabbing = !gesture.pinching &&
                    (HomeSpaceDeskState.hasActiveGesture() || DeskLassoState.active)
                val mode = GlassesLookMode.effective()
                if (mode == GlassesLookMode.GESTURE && grabbing) {
                    Unit
                } else {
                    val sign = GlassesLookMode.lookPanSign(mode)
                    GlassesHomeLook.addLookDegrees(
                        yawDeltaDeg = sign * HomeSpaceScene.fpsYawDegreesDelta(
                            deltaXNorm = action.dxPx / viewportW,
                            viewportWidthPx = viewportW,
                            viewportHeightPx = viewportH,
                        ),
                        pitchDeltaDeg = sign * HomeSpaceScene.fpsPitchDelta(action.dyPx / viewportH),
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
        if (LauncherContextMenuState.isOpen) return false
        lastViewportW = viewportW.coerceAtLeast(1f)
        lastViewportH = viewportH.coerceAtLeast(1f)
        val nx = (x / lastViewportW).coerceIn(0f, 1f)
        val ny = (y / lastViewportH).coerceIn(0f, 1f)
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
                applyAction(action, lastViewportW, lastViewportH, onZoomSphere)
            }
        }

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (pointerCount >= 2) {
                    cancelLongPress()
                    val dist = hypot(x - secondX, y - secondY)
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
                    if (allowDesk && !fpsLook) scheduleLongPress()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerCount >= 2) {
                    cancelLongPress()
                    val dist = hypot(x - secondX, y - secondY)
                    val midX = (x + secondX) * 0.5f
                    val midY = (y + secondY) * 0.5f
                    if (!gesture.pinching) {
                        apply(listOf(gesture.onPinchBegin(dist, midX, midY)))
                    }
                    apply(
                        gesture.onPinchMove(
                            dist,
                            midX,
                            midY,
                            gestureLook = gestureLook,
                        ),
                    )
                } else {
                    val actions = gesture.onMove(x, y, allowDesk, fpsLook, dialogOpen, gestureLook)
                    if (actions.any { it === BumpDeskHostAction.DeskMoveWhilePressed }) {
                        cancelLongPress()
                    }
                    apply(actions)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
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
