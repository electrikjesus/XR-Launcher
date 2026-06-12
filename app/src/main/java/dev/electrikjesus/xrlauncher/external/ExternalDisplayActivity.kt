package dev.electrikjesus.xrlauncher.external

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.ui.external.ExternalDisplayWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme

/**
 * Workspace for wired external displays (e.g. SmartGlasses in Desktop Mode).
 * Does not require [android.hardware.display.category.XR_PROJECTED].
 */
class ExternalDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveFullscreen()

        val displayId = display?.displayId ?: Display.DEFAULT_DISPLAY
        if (isDebugBuild()) {
            Log.d(TAG, "onCreate displayId=$displayId")
        }

        val appRepository = AppRepository(this)
        val appLauncher = AppLauncher(this)
        val apps = appRepository.loadLaunchableApps()

        setContent {
            XRLauncherTheme(forGlasses = true) {
                ExternalDisplayWorkspaceScreen(
                    apps = apps,
                    onLaunchApp = { app ->
                        appLauncher.launchOnDisplay(app.componentName, displayId)
                    },
                )
            }
        }

        window.decorView.post {
            if (isDebugBuild()) {
                val bounds = window.decorView.rootView.layoutParams
                Log.d(
                    TAG,
                    "window layout width=${bounds?.width} height=${bounds?.height} display=$displayId",
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen()
        }
    }

    private fun applyImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val TAG = "XRLauncher/Display"
    }

    private fun isDebugBuild(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
