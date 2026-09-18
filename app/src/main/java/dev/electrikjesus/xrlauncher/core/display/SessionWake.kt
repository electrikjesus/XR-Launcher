package dev.electrikjesus.xrlauncher.core.display

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity

/**
 * Keep the host and glasses displays awake for the session. Phone sleep was
 * turning the SmartGlasses overlay OFF even while the launcher was in front.
 */
object SessionWake {
    fun keepDisplayOn(activity: ComponentActivity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        }
    }
}
