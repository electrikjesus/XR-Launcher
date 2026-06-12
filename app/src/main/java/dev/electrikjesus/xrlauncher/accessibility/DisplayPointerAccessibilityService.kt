package dev.electrikjesus.xrlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.input.PointerButton

/**
 * Injects tap gestures on a secondary display so the phone companion can drive
 * a desktop-style pointer on glasses. User must enable this service in Settings.
 */
class DisplayPointerAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        DisplayPointerInjector.service = this
        Log.d(TAG, "Display pointer service connected")
    }

    override fun onDestroy() {
        DisplayPointerInjector.service = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun dispatchClick(
        displayId: Int,
        normalizedX: Float,
        normalizedY: Float,
        button: PointerButton,
    ): Boolean {
        val displayManager = getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId) ?: return false
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        val x = (normalizedX * metrics.widthPixels).coerceIn(0f, metrics.widthPixels.toFloat())
        val y = (normalizedY * metrics.heightPixels).coerceIn(0f, metrics.heightPixels.toFloat())

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS)
        val gesture = GestureDescription.Builder()
            .setDisplayId(displayId)
            .addStroke(stroke)
            .build()

        Log.d(TAG, "dispatchClick display=$displayId ($x,$y) button=$button")
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        private const val TAG = "XRLauncher/DisplayPointer"
        private const val TAP_DURATION_MS = 50L
    }
}
