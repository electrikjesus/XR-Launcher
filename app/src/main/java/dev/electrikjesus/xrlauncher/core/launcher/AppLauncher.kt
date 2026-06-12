package dev.electrikjesus.xrlauncher.core.launcher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display

class AppLauncher(private val context: Context) {
    fun launchOnDefaultDisplay(componentName: ComponentName) {
        launchOnDisplay(componentName, Display.DEFAULT_DISPLAY)
    }

    fun launchOnGlasses(componentName: ComponentName, preferredDisplayId: Int? = null) {
        val displayId = preferredDisplayId ?: findSecondaryDisplayId() ?: Display.DEFAULT_DISPLAY
        launchOnDisplay(componentName, displayId)
    }

    fun launchOnDisplay(componentName: ComponentName, displayId: Int) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = componentName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle()
        context.startActivity(intent, options)
    }

    fun findSecondaryDisplayId(): Int? {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        return displayManager.displays
            .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }
            ?.displayId
    }
}
