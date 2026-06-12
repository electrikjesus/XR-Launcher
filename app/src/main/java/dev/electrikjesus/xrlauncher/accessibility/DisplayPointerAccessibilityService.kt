package dev.electrikjesus.xrlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Log
import dev.electrikjesus.xrlauncher.core.display.GlassesControlMode
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Injects tap gestures and draws a system overlay cursor on the glasses display.
 * User must enable this service in Settings.
 */
class DisplayPointerAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayManager: DisplayCursorOverlayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        DisplayPointerInjector.service = this
        overlayManager = DisplayCursorOverlayManager(this)
        serviceScope.launch {
            combine(
                CompanionPointerBus.cursor,
                CompanionPointerBus.glassesControlMode,
            ) { cursor, mode -> cursor to mode }
                .collect { (cursor, mode) ->
                    syncOverlay(cursor.x, cursor.y, cursor.isPressed, mode)
                }
        }
        Log.d(TAG, "Display pointer service connected")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        overlayManager?.detach()
        overlayManager = null
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

    private fun syncOverlay(
        normalizedX: Float,
        normalizedY: Float,
        pressed: Boolean,
        mode: GlassesControlMode,
    ) {
        val manager = overlayManager ?: return
        val displayId = GlassesSessionState.secondaryDisplayId
        if (displayId == null) {
            manager.detach()
            return
        }
        // Overlay cursor in Desktop mode (controlling other apps). Launcher uses in-activity dot.
        if (mode != GlassesControlMode.DESKTOP) {
            manager.detach()
            return
        }
        manager.attach(displayId)
        manager.update(normalizedX, normalizedY, pressed)
    }

    companion object {
        private const val TAG = "XRLauncher/DisplayPointer"
        private const val TAP_DURATION_MS = 50L
    }
}
