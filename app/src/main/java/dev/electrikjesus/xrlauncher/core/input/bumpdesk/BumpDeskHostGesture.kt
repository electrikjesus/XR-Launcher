package dev.electrikjesus.xrlauncher.core.input.bumpdesk

import kotlin.math.hypot

/**
 * Portable subset of BumpDesk [com.bass.bumpdesk.LauncherActivity] gesture routing.
 *
 * BumpDesk feeds absolute screen coords into InteractionManager after touch-slop, uses
 * middle-button drag for camera pan, secondary for context, and two-finger for pan+zoom.
 * This classifier does not raycast or own desk state — it only decides what the host
 * pointer layer should emit each frame.
 */
class BumpDeskHostGesture(
    /** Pixel distance before a primary press becomes a desk drag (BumpDesk touchSlop). */
    var touchSlopPx: Float = DEFAULT_TOUCH_SLOP_PX,
) {
    var downX: Float = 0f
        private set
    var downY: Float = 0f
        private set
    var lastX: Float = 0f
        private set
    var lastY: Float = 0f
        private set

    var primaryDown: Boolean = false
        private set
    /** True when the press started off chrome / modals (desk may grab after slop). */
    var deskGrabAllowed: Boolean = false
        private set
    var deskDragArmed: Boolean = false
        private set
    var middleDragging: Boolean = false
        private set
    var pinching: Boolean = false
        private set
    var pinchDistance: Float = 0f
        private set

    fun reset() {
        primaryDown = false
        deskGrabAllowed = false
        deskDragArmed = false
        middleDragging = false
        pinching = false
        pinchDistance = 0f
        downX = 0f
        downY = 0f
        lastX = 0f
        lastY = 0f
    }

    fun onPrimaryDown(x: Float, y: Float, allowDeskGrab: Boolean): List<BumpDeskHostAction> {
        primaryDown = true
        deskGrabAllowed = allowDeskGrab
        deskDragArmed = allowDeskGrab
        middleDragging = false
        pinching = false
        downX = x
        downY = y
        lastX = x
        lastY = y
        val out = mutableListOf<BumpDeskHostAction>(BumpDeskHostAction.CursorAt(x, y))
        // Hold-Left starts on down (XR desk grab); absolute cursor — no FPS re-lock.
        // Touch-slop still gates LookPan while pressed and micro-jitter before move callbacks.
        if (allowDeskGrab) {
            out += BumpDeskHostAction.BeginDeskHold
        }
        return out
    }

    fun onSecondaryDown(x: Float, y: Float): BumpDeskHostAction {
        lastX = x
        lastY = y
        return BumpDeskHostAction.RightClick(x, y)
    }

    fun onMiddleDown(x: Float, y: Float): BumpDeskHostAction {
        middleDragging = true
        primaryDown = false
        deskDragArmed = false
        pinching = false
        lastX = x
        lastY = y
        return BumpDeskHostAction.CursorAt(x, y)
    }

    fun onPinchBegin(distance: Float, midX: Float, midY: Float): BumpDeskHostAction {
        if (deskDragArmed) {
            // Caller must end any active desk hold before pinch.
        }
        pinching = true
        primaryDown = false
        deskDragArmed = false
        middleDragging = false
        pinchDistance = distance
        lastX = midX
        lastY = midY
        return BumpDeskHostAction.CancelDeskHold
    }

    /**
     * @return actions to apply in order (may be empty).
     */
    fun onMove(
        x: Float,
        y: Float,
        allowDeskGrab: Boolean,
        fpsLook: Boolean,
        dialogOpen: Boolean,
    ): List<BumpDeskHostAction> {
        val dx = x - lastX
        val dy = y - lastY
        lastX = x
        lastY = y
        val out = mutableListOf<BumpDeskHostAction>(BumpDeskHostAction.CursorAt(x, y))

        when {
            middleDragging -> {
                if (dx != 0f || dy != 0f) {
                    out += BumpDeskHostAction.LookPan(dx, dy)
                }
            }
            primaryDown && deskDragArmed -> {
                val dist = hypot(x - downX, y - downY)
                if (dist > touchSlopPx) {
                    out += BumpDeskHostAction.DeskMoveWhilePressed
                }
            }
            primaryDown && !deskDragArmed -> {
                // Pressed on chrome / modal: Compose owns the hit; no look-steal.
            }
            !primaryDown && !middleDragging && !pinching && fpsLook && !dialogOpen -> {
                if (dx != 0f || dy != 0f) {
                    out += BumpDeskHostAction.LookPan(dx, dy)
                }
            }
        }
        return out
    }

    fun onPinchMove(distance: Float, midX: Float, midY: Float): List<BumpDeskHostAction> {
        if (!pinching) return emptyList()
        val prev = pinchDistance
        pinchDistance = distance
        lastX = midX
        lastY = midY
        return listOf(
            BumpDeskHostAction.CursorAt(midX, midY),
            BumpDeskHostAction.PinchZoom(prev, distance),
        )
    }

    fun onPrimaryUp(x: Float, y: Float): List<BumpDeskHostAction> {
        lastX = x
        lastY = y
        val out = mutableListOf<BumpDeskHostAction>(BumpDeskHostAction.CursorAt(x, y))
        if (primaryDown && deskDragArmed) {
            out += BumpDeskHostAction.EndDeskHold
        }
        // Chrome / modal presses: leave for Compose clickables (no bus click).
        primaryDown = false
        deskGrabAllowed = false
        deskDragArmed = false
        return out
    }

    fun onMiddleUp(x: Float, y: Float): BumpDeskHostAction {
        middleDragging = false
        lastX = x
        lastY = y
        return BumpDeskHostAction.CursorAt(x, y)
    }

    fun onPinchEnd(): BumpDeskHostAction {
        pinching = false
        pinchDistance = 0f
        return BumpDeskHostAction.None
    }

    companion object {
        const val DEFAULT_TOUCH_SLOP_PX = 24f
    }
}

sealed class BumpDeskHostAction {
    data object None : BumpDeskHostAction()
    data class CursorAt(val x: Float, val y: Float) : BumpDeskHostAction()
    data object BeginDeskHold : BumpDeskHostAction()
    data object EndDeskHold : BumpDeskHostAction()
    data object DeskMoveWhilePressed : BumpDeskHostAction()
    data object CancelDeskHold : BumpDeskHostAction()
    data class LeftClick(val x: Float, val y: Float) : BumpDeskHostAction()
    data class RightClick(val x: Float, val y: Float) : BumpDeskHostAction()
    data class LookPan(val dxPx: Float, val dyPx: Float) : BumpDeskHostAction()
    data class PinchZoom(val previousDistance: Float, val currentDistance: Float) : BumpDeskHostAction()
}
