package dev.electrikjesus.xrlauncher.glasses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme

class GlassesLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appRepository = AppRepository(this)
        val appLauncher = AppLauncher(this)
        val apps = appRepository.loadLaunchableApps()

        setContent {
            XRLauncherTheme(forGlasses = true) {
                GlassesWorkspaceScreen(
                    apps = apps,
                    onLaunchApp = { app ->
                        appLauncher.launchOnDefaultDisplay(app.componentName)
                    },
                )
            }
        }
    }
}
