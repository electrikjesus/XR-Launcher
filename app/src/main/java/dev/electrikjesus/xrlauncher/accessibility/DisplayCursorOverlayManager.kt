package dev.electrikjesus.xrlauncher.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * Draws the companion cursor on a secondary display via [WindowManager.TYPE_ACCESSIBILITY_OVERLAY].
 * Stays visible over Settings, Play Store, etc. — not tied to [ExternalDisplayActivity].
 */
class DisplayCursorOverlayManager(
    private val context: Context,
) {
    private var overlayView: CursorOverlayView? = null
    private var windowManager: WindowManager? = null
    private var attachedDisplayId: Int? = null

    fun attach(displayId: Int) {
        if (attachedDisplayId == displayId && overlayView != null) return
        detach()
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId)
        if (display == null) {
            Log.w(TAG, "Cannot attach overlay — display $displayId missing")
            return
        }
        val displayContext = context.createDisplayContext(display)
        val wm = displayContext.getSystemService(WindowManager::class.java)
        val view = CursorOverlayView(displayContext)
        wm.addView(view, overlayLayoutParams())
        overlayView = view
        windowManager = wm
        attachedDisplayId = displayId
        Log.d(TAG, "Cursor overlay attached on display $displayId")
    }

    fun update(normalizedX: Float, normalizedY: Float, pressed: Boolean) {
        overlayView?.setCursor(normalizedX, normalizedY, pressed)
    }

    fun detach() {
        val wm = windowManager
        val view = overlayView
        if (wm != null && view != null) {
            try {
                wm.removeView(view)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Overlay already removed", e)
            }
        }
        overlayView = null
        windowManager = null
        attachedDisplayId = null
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

    private class CursorOverlayView(context: Context) : View(context) {
        private var normalizedX = 0.5f
        private var normalizedY = 0.5f
        private var pressed = false
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
        }

        fun setCursor(x: Float, y: Float, isPressed: Boolean) {
            normalizedX = x.coerceIn(0f, 1f)
            normalizedY = y.coerceIn(0f, 1f)
            pressed = isPressed
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (width <= 0 || height <= 0) return
            val cx = normalizedX * width
            val cy = normalizedY * height
            val radius = (width.coerceAtMost(height) * 0.009f).coerceIn(6f, 14f)
            val fillAlpha = if (pressed) 0.65f else 0.45f
            fillPaint.color = if (pressed) {
                Color.parseColor("#BB86FC")
            } else {
                Color.parseColor("#03DAC5")
            }
            fillPaint.alpha = (fillAlpha * 255).toInt()
            ringPaint.alpha = (0.55f * 255).toInt()
            canvas.drawCircle(cx, cy, radius, fillPaint)
            canvas.drawCircle(cx, cy, radius + 1.5f, ringPaint)
        }
    }

    companion object {
        private const val TAG = "XRLauncher/CursorOverlay"
    }
}
