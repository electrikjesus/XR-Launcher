package dev.electrikjesus.xrlauncher.external

import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )

        val displayId = display?.displayId ?: Display.DEFAULT_DISPLAY
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
    }
}
