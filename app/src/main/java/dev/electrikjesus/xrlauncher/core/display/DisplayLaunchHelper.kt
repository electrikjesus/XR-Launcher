package dev.electrikjesus.xrlauncher.core.display

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import dev.electrikjesus.xrlauncher.companion.CompanionControllerActivity
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.external.ExternalDisplayActivity

object DisplayLaunchHelper {
    fun findSecondaryDisplayId(context: Context): Int? {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        return displayManager.displays
            .filter { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }
            .maxByOrNull { it.width * it.height }
            ?.displayId
    }

    fun displayBounds(context: Context, displayId: Int): Rect? {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId) ?: return null
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    fun launchActivityOnDisplay(
        context: Context,
        activityClass: Class<*>,
        displayId: Int,
    ): Boolean {
        if (displayId == Display.DEFAULT_DISPLAY) return false
        val intent = Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val options = ActivityOptions.makeBasic()
        options.launchDisplayId = displayId
        displayBounds(context, displayId)?.let { bounds ->
            options.setLaunchBounds(bounds)
        }
        context.startActivity(intent, options.toBundle())
        return true
    }

    fun launchActivityOnDefaultDisplay(
        context: Context,
        activityClass: Class<*>,
        clearTop: Boolean = false,
    ) {
        val intent = Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (clearTop) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        val options = ActivityOptions.makeBasic()
        options.launchDisplayId = Display.DEFAULT_DISPLAY
        context.startActivity(intent, options.toBundle())
    }

    fun openCompanionController(context: Context) {
        launchActivityOnDefaultDisplay(
            context = context,
            activityClass = CompanionControllerActivity::class.java,
            clearTop = true,
        )
    }

    /** Launch workspace on glasses display and companion touchpad on the phone. */
    fun openGlassesSession(context: Context, displayId: Int): Boolean {
        CompanionPointerBus.resetCursor()
        CompanionPointerBus.setMotionControlEnabled(false)

        // Pin each activity to its display — Desktop Mode otherwise follows the external launch.
        openCompanionController(context)
        return launchActivityOnDisplay(
            context = context,
            activityClass = ExternalDisplayActivity::class.java,
            displayId = displayId,
        )
    }
}
