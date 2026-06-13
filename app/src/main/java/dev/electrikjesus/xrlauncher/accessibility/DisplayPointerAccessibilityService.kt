package dev.electrikjesus.xrlauncher.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Log
import dev.electrikjesus.xrlauncher.core.display.GlassesControlMode
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.display.LauncherInjectFrame
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        overlayManager = DisplayCursorOverlayManager(this) {
            DisplayLaunchHelper.showLauncherOnGlasses(this)
        }
        if (GlassesSessionState.secondaryDisplayId != null &&
            GlassesSessionState.xrInputMode != GlassesXrInputMode.GLASSES_HEAD_TRACKING
        ) {
            GlassesSessionState.controlMode = GlassesControlMode.DESKTOP
            CompanionPointerBus.setGlassesControlMode(GlassesControlMode.DESKTOP)
        }
        syncTextEntryActive()
        serviceScope.launch {
            CompanionPointerBus.cursor.collect { cursor ->
                syncOverlay(cursor.x, cursor.y, cursor.isPressed)
            }
        }
        serviceScope.launch {
            GlassesSessionState.launcherForegroundFlow.collect { foreground ->
                overlayManager?.setLauncherForeground(foreground)
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

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            -> syncTextEntryActive()
        }
    }

    override fun onInterrupt() = Unit

    fun dispatchClick(
        displayId: Int,
        normalizedX: Float,
        normalizedY: Float,
        button: PointerButton,
        mapViaLauncherFrame: Boolean = false,
    ): Boolean {
        val displayManager = getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId) ?: return false
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        val (x, y) = normalizedToDisplayPixels(normalizedX, normalizedY, metrics, mapViaLauncherFrame)

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS)
        val gesture = GestureDescription.Builder()
            .setDisplayId(displayId)
            .addStroke(stroke)
            .build()

        Log.d(TAG, "dispatchClick display=$displayId ($x,$y) button=$button")
        return dispatchGesture(gesture, null, null)
    }

    fun dispatchDrag(
        displayId: Int,
        fromNormalizedX: Float,
        fromNormalizedY: Float,
        toNormalizedX: Float,
        toNormalizedY: Float,
        mapViaLauncherFrame: Boolean = false,
    ): Boolean {
        val displayManager = getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId) ?: return false
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        val (x1, y1) = normalizedToDisplayPixels(fromNormalizedX, fromNormalizedY, metrics, mapViaLauncherFrame)
        val (x2, y2) = normalizedToDisplayPixels(toNormalizedX, toNormalizedY, metrics, mapViaLauncherFrame)

        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, DRAG_DURATION_MS)
        val gesture = GestureDescription.Builder()
            .setDisplayId(displayId)
            .addStroke(stroke)
            .build()

        Log.d(TAG, "dispatchDrag display=$displayId ($x1,$y1)->($x2,$y2)")
        return dispatchGesture(gesture, null, null)
    }

    private fun normalizedToDisplayPixels(
        normalizedX: Float,
        normalizedY: Float,
        metrics: DisplayMetrics,
        mapViaLauncherFrame: Boolean,
    ): Pair<Float, Float> {
        if (mapViaLauncherFrame && GlassesSessionState.launcherForeground) {
            val frame: LauncherInjectFrame = GlassesSessionState.launcherInjectFrame
            if (frame.isValid()) {
                return frame.toDisplayPixels(normalizedX, normalizedY)
            }
        }
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()
        return Pair(
            (normalizedX * w).coerceIn(0f, w),
            (normalizedY * h).coerceIn(0f, h),
        )
    }

    private fun syncOverlay(
        normalizedX: Float,
        normalizedY: Float,
        pressed: Boolean,
    ) {
        val manager = overlayManager ?: return
        val displayId = GlassesSessionState.secondaryDisplayId
        if (displayId == null) {
            manager.detach()
            return
        }
        manager.attach(displayId)
        manager.update(normalizedX, normalizedY, pressed)
    }

    private fun syncTextEntryActive() {
        val imeVisible = windows?.any { window ->
            window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
        } == true
        val displayId = GlassesSessionState.secondaryDisplayId
        val popupLikelyOpen = if (displayId != null) {
            val appWindowsOnDisplay = windows?.count { window ->
                window.displayId == displayId &&
                    window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION
            } ?: 0
            appWindowsOnDisplay > 1
        } else {
            false
        }
        CompanionPointerBus.setTextEntryActive(imeVisible || popupLikelyOpen)
    }

    companion object {
        private const val TAG = "XRLauncher/DisplayPointer"
        private const val TAP_DURATION_MS = 50L
        private const val DRAG_DURATION_MS = 250L
    }
}
