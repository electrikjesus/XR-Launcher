package dev.electrikjesus.xrlauncher.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import dev.electrikjesus.xrlauncher.R

/**
 * Draws the companion cursor and a return-to-launcher bubble on the glasses display.
 */
class DisplayCursorOverlayManager(
    private val context: Context,
    private val onReturnToLauncher: () -> Unit,
) {
    private var overlayRoot: FrameLayout? = null
    private var cursorView: CursorOverlayView? = null
    private var bubbleView: LauncherReturnBubbleView? = null
    private var windowManager: WindowManager? = null
    private var attachedDisplayId: Int? = null
    private var launcherForeground = true

    fun attach(displayId: Int) {
        if (attachedDisplayId == displayId && overlayRoot != null) return
        detach()
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId)
        if (display == null) {
            Log.w(TAG, "Cannot attach overlay — display $displayId missing")
            return
        }
        val displayContext = context.createDisplayContext(display)
        val wm = displayContext.getSystemService(WindowManager::class.java)
        val root = FrameLayout(displayContext)
        val cursor = CursorOverlayView(displayContext)
        val bubble = LauncherReturnBubbleView(displayContext) {
            onReturnToLauncher()
        }
        root.addView(
            cursor,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        root.addView(
            bubble,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END,
            ).apply {
                val margin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20f,
                    displayContext.resources.displayMetrics,
                ).toInt()
                setMargins(margin, margin, margin, margin)
            },
        )
        wm.addView(root, overlayLayoutParams())
        overlayRoot = root
        cursorView = cursor
        bubbleView = bubble
        windowManager = wm
        attachedDisplayId = displayId
        bubble.visibility = if (launcherForeground) View.GONE else View.VISIBLE
        Log.d(TAG, "Cursor overlay attached on display $displayId")
    }

    fun update(normalizedX: Float, normalizedY: Float, pressed: Boolean) {
        cursorView?.setCursor(normalizedX, normalizedY, pressed)
    }

    fun setLauncherForeground(foreground: Boolean) {
        launcherForeground = foreground
        bubbleView?.visibility = if (foreground) View.GONE else View.VISIBLE
    }

    fun detach() {
        val wm = windowManager
        val view = overlayRoot
        if (wm != null && view != null) {
            try {
                wm.removeView(view)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Overlay already removed", e)
            }
        }
        overlayRoot = null
        cursorView = null
        bubbleView = null
        windowManager = null
        attachedDisplayId = null
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
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

        init {
            isClickable = false
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

    private class LauncherReturnBubbleView(
        context: Context,
        onClick: () -> Unit,
    ) : FrameLayout(context) {
        init {
            isClickable = true
            isFocusable = true
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1A1520"))
                setStroke(2, Color.parseColor("#03DAC5"))
            }
            background = bg
            val size = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                56f,
                resources.displayMetrics,
            ).toInt()
            minimumWidth = size
            minimumHeight = size
            val label = TextView(context).apply {
                text = context.getString(R.string.launcher_return_bubble_label)
                setTextColor(Color.parseColor("#03DAC5"))
                textSize = 11f
                gravity = Gravity.CENTER
            }
            addView(
                label,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                ),
            )
            setOnClickListener { onClick() }
            elevation = 12f
        }
    }

    companion object {
        private const val TAG = "XRLauncher/CursorOverlay"
    }
}
