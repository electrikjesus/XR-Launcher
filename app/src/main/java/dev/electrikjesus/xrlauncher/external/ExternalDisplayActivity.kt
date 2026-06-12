package dev.electrikjesus.xrlauncher.external

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.external.ExternalDisplayWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme
import kotlinx.coroutines.launch

class ExternalDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveFullscreen()

        val displayId = display?.displayId ?: Display.DEFAULT_DISPLAY
        GlassesSessionState.secondaryDisplayId = displayId
        if (isDebugBuild()) {
            Log.d(TAG, "onCreate displayId=$displayId")
        }

        val appRepository = AppRepository(this)
        val appLauncher = AppLauncher(this)
        val workspaceRepository = WorkspaceRepository(applicationContext)
        val apps = appRepository.loadLaunchableApps()
            .filter { it.packageName != packageName }

        setContent {
            val scope = rememberCoroutineScope()
            val repo = remember { workspaceRepository }
            XRLauncherTheme(forGlasses = true) {
                ExternalDisplayWorkspaceScreen(
                    apps = apps,
                    workspaceRepository = repo,
                    onLaunchApp = { app ->
                        Log.d(TAG, "Launching ${app.label} on displayId=$displayId")
                        appLauncher.launchOnDisplay(app.componentName, displayId)
                        window.decorView.post { moveTaskToBack(true) }
                    },
                    onToggleHotseatPin = { app ->
                        scope.launch {
                            repo.toggleHotseatPin(app.componentKey())
                            Log.d(TAG, "Toggled hotseat pin for ${app.label}")
                        }
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

    override fun onResume() {
        super.onResume()
        GlassesSessionState.launcherForeground = true
    }

    override fun onPause() {
        GlassesSessionState.launcherForeground = false
        super.onPause()
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
