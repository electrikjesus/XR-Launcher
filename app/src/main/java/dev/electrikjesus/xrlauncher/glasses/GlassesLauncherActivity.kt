package dev.electrikjesus.xrlauncher.glasses

import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.electrikjesus.xrlauncher.core.display.SessionWake
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlassesLauncherActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionWake.keepDisplayOn(this)
        val appLauncher = AppLauncher(this)
        val workspaceRepository = WorkspaceRepository(applicationContext)

        setContent {
            val repo = remember { workspaceRepository }
            XRLauncherTheme(forGlasses = true) {
                GlassesWorkspaceScreen(
                    launcherPackageName = packageName,
                    workspaceRepository = repo,
                    onLaunchApp = { app ->
                        appLauncher.launchOnDefaultDisplay(app.componentName)
                    },
                    onToggleHotseatPin = { app ->
                        scope.launch {
                            repo.toggleHotseatPin(app.componentKey())
                        }
                    },
                )
            }
        }
    }
}
