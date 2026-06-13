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
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.LauncherInjectFrame

/**
 * Draws the companion cursor (passthrough overlay) and an optional return-to-launcher bubble.
 *
 * The cursor layer is always [FLAG_NOT_TOUCHABLE] so injected gestures reach apps and Compose
 * below. The bubble lives in a separate small touchable window when the launcher is backgrounded.
 */
class DisplayCursorOverlayManager(
    private val context: Context,
    private val onReturnToLauncher: () -> Unit,
) {
    private var cursorRoot: FrameLayout? = null
    private var cursorView: CursorOverlayView? = null
    private var bubbleView: LauncherReturnBubbleView? = null
    private var cursorWindowManager: WindowManager? = null
    private var bubbleWindowManager: WindowManager? = null
    private var attachedDisplayId: Int? = null
    private var launcherForeground = true

    fun attach(displayId: Int) {
        if (attachedDisplayId == displayId && cursorRoot != null) return
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
        root.addView(
            cursor,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        wm.addView(root, cursorOverlayLayoutParams())
        cursorRoot = root
        cursorView = cursor
        cursorWindowManager = wm
        attachedDisplayId = displayId
        syncBubbleVisibility()
        Log.d(TAG, "Cursor overlay attached on display $displayId (passthrough)")
    }

    fun update(normalizedX: Float, normalizedY: Float, pressed: Boolean) {
        cursorView?.setCursor(
            x = normalizedX,
            y = normalizedY,
            isPressed = pressed,
            injectFrame = GlassesSessionState.launcherInjectFrame,
            mapViaLauncherFrame = GlassesSessionState.launcherForeground &&
                !GlassesSessionState.launcherBackgrounded,
        )
    }

    fun setLauncherForeground(foreground: Boolean) {
        if (launcherForeground == foreground) return
        launcherForeground = foreground
        syncBubbleVisibility()
    }

    fun detach() {
        detachBubble()
        val wm = cursorWindowManager
        val view = cursorRoot
        if (wm != null && view != null) {
            try {
                wm.removeView(view)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Cursor overlay already removed", e)
            }
        }
        cursorRoot = null
        cursorView = null
        cursorWindowManager = null
        attachedDisplayId = null
    }

    private fun syncBubbleVisibility() {
        if (launcherForeground) {
            detachBubble()
        } else {
            attachBubble()
        }
    }

    private fun attachBubble() {
        if (bubbleView != null) return
        val displayId = attachedDisplayId ?: return
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId) ?: return
        val displayContext = context.createDisplayContext(display)
        val wm = displayContext.getSystemService(WindowManager::class.java)
        val bubble = LauncherReturnBubbleView(displayContext) { onReturnToLauncher() }
        wm.addView(bubble, bubbleOverlayLayoutParams(displayContext))
        bubbleView = bubble
        bubbleWindowManager = wm
    }

    private fun detachBubble() {
        val wm = bubbleWindowManager
        val view = bubbleView
        if (wm != null && view != null) {
            try {
                wm.removeView(view)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Bubble overlay already removed", e)
            }
        }
        bubbleView = null
        bubbleWindowManager = null
    }

    private fun cursorOverlayLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

    private fun bubbleOverlayLayoutParams(displayContext: Context): WindowManager.LayoutParams {
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            20f,
            displayContext.resources.displayMetrics,
        ).toInt()
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = margin
            y = margin
        }
    }

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

        fun setCursor(
            x: Float,
            y: Float,
            isPressed: Boolean,
            injectFrame: LauncherInjectFrame,
            mapViaLauncherFrame: Boolean,
        ) {
            normalizedX = x.coerceIn(0f, 1f)
            normalizedY = y.coerceIn(0f, 1f)
            pressed = isPressed
            frame = injectFrame
            useLauncherFrame = mapViaLauncherFrame && injectFrame.isValid()
            invalidate()
        }

        private var frame = LauncherInjectFrame()
        private var useLauncherFrame = false

        fun setCursor(x: Float, y: Float, isPressed: Boolean) {
            setCursor(x, y, isPressed, LauncherInjectFrame(), mapViaLauncherFrame = false)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (width <= 0 || height <= 0) return
            val (cx, cy) = if (useLauncherFrame) {
                frame.toDisplayPixels(normalizedX, normalizedY)
            } else {
                Pair(normalizedX * width, normalizedY * height)
            }
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
