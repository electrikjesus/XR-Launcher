package dev.electrikjesus.xrlauncher.core.display

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import dev.electrikjesus.xrlauncher.companion.CompanionControllerActivity
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.external.ExternalDisplayActivity

object DisplayLaunchHelper {
    private const val TAG = "XRLauncher/Display"
    private const val FEATURE_XR_API_SPATIAL = "android.software.xr.api.spatial"

    fun findSecondaryDisplayId(context: Context): Int? {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        return displayManager.displays
            .filter { display -> display.isValidSecondaryTarget(displayManager) }
            .maxByOrNull { it.width * it.height }
            ?.displayId
    }

    /** Resolve a live secondary display — IDs change when glasses reconnect. */
    fun resolveSecondaryDisplayId(context: Context, preferredId: Int? = null): Int? {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        if (preferredId != null) {
            displayManager.getDisplay(preferredId)?.let { display ->
                if (display.isValidSecondaryTarget(displayManager)) return preferredId
            }
        }
        return findSecondaryDisplayId(context)
    }

    fun launchActivityOnDisplay(
        context: Context,
        activityClass: Class<*>,
        displayId: Int,
    ): Boolean {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId)
        if (display == null || !display.isValidSecondaryTarget(displayManager)) {
            Log.w(TAG, "Refusing launch on invalid displayId=$displayId")
            return false
        }
        val intent = Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val options = buildLaunchOptions(displayManager, displayId)
        return try {
            context.startActivity(intent, options.toBundle())
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied launching ${activityClass.simpleName} on display $displayId", e)
            false
        }
    }

    fun launchActivityOnDefaultDisplay(
        context: Context,
        activityClass: Class<*>,
        clearTop: Boolean = false,
    ): Boolean {
        val intent = Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (clearTop) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        val options = ActivityOptions.makeBasic()
        options.launchDisplayId = Display.DEFAULT_DISPLAY
        return try {
            context.startActivity(intent, options.toBundle())
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied launching ${activityClass.simpleName} on default display", e)
            false
        }
    }

    fun openCompanionController(context: Context): Boolean {
        return launchActivityOnDefaultDisplay(
            context = context,
            activityClass = CompanionControllerActivity::class.java,
            clearTop = true,
        )
    }

    /** Launch workspace on glasses display and companion touchpad on the phone. */
    fun openGlassesSession(context: Context, preferredDisplayId: Int? = null): Boolean {
        val displayId = resolveSecondaryDisplayId(context, preferredDisplayId)
        if (displayId == null) {
            Log.w(TAG, "No secondary display available for glasses session")
            return false
        }

        CompanionPointerBus.resetCursor()
        CompanionPointerBus.setMotionControlEnabled(false)
        GlassesSessionState.secondaryDisplayId = displayId
        GlassesSessionState.preferSubspaceShell =
            context.packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)
        applySessionControlMode()
        Log.d(
            TAG,
            "Session displayId=$displayId subspace=${GlassesSessionState.preferSubspaceShell}",
        )

        openCompanionController(context)
        launchActivityOnDisplay(
            context = context,
            activityClass = ExternalDisplayActivity::class.java,
            displayId = displayId,
        )
        // CLEAR_TOP relaunch lands fullscreen on Desktop Mode (not freeform) — verified on Pixel 8.
        val fullscreen = showLauncherOnGlasses(context)
        if (fullscreen) {
            Log.d(TAG, "Opened glasses session on displayId=$displayId (fullscreen relaunch)")
        }
        return fullscreen
    }

    fun showLauncherOnGlasses(context: Context): Boolean {
        val displayId = resolveSecondaryDisplayId(context, GlassesSessionState.secondaryDisplayId)
            ?: return false
        applySessionControlMode()
        val intent = Intent(context, ExternalDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val options = buildLaunchOptions(displayManager, displayId)
        return try {
            context.startActivity(intent, options.toBundle())
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to show launcher on glasses", e)
            false
        }
    }

    private fun applySessionControlMode() {
        val mode = if (DisplayPointerInjector.isAvailable) {
            GlassesControlMode.DESKTOP
        } else {
            GlassesControlMode.LAUNCHER
        }
        GlassesSessionState.controlMode = mode
        CompanionPointerBus.setGlassesControlMode(mode)
    }

    private fun buildLaunchOptions(displayManager: DisplayManager, displayId: Int): ActivityOptions {
        val options = ActivityOptions.makeBasic()
        options.launchDisplayId = displayId
        val display = displayManager.getDisplay(displayId) ?: return options
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        options.setLaunchBounds(Rect(0, 0, metrics.widthPixels, metrics.heightPixels))
        Log.d(TAG, "Launch options displayId=$displayId size=${metrics.widthPixels}x${metrics.heightPixels}")
        return options
    }

    private fun Display.isValidSecondaryTarget(displayManager: DisplayManager): Boolean {
        if (displayId == Display.DEFAULT_DISPLAY) return false
        if (state == Display.STATE_OFF) return false
        return displayManager.displays.any { it.displayId == displayId }
    }
}
