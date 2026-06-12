package dev.electrikjesus.xrlauncher.external

import android.content.Intent
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
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.LauncherInjectFrame
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.external.ExternalDisplayWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme
import kotlinx.coroutines.launch

class ExternalDisplayActivity : ComponentActivity() {
    private var lastLaunchAtMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveFullscreen()
        syncSessionDisplayId()

        val displayId = display?.displayId ?: Display.DEFAULT_DISPLAY
        if (isDebugBuild()) {
            Log.d(TAG, "onCreate displayId=$displayId saved=${savedInstanceState != null}")
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
                    onLaunchApp = { app -> launchApp(appLauncher, app) },
                    onToggleHotseatPin = { app ->
                        scope.launch {
                            repo.toggleHotseatPin(app.componentKey())
                            Log.d(TAG, "Toggled hotseat pin for ${app.label}")
                        }
                    },
                )
            }
        }

        window.decorView.viewTreeObserver.addOnGlobalLayoutListener { updateInjectFrame() }
        window.decorView.post { updateInjectFrame() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        syncSessionDisplayId()
        window.decorView.post { updateInjectFrame() }
        if (isDebugBuild()) {
            Log.d(TAG, "onNewIntent displayId=${display?.displayId} (reused instance)")
        }
    }

    override fun onResume() {
        super.onResume()
        GlassesSessionState.launcherForeground = true
        syncSessionDisplayId()
        window.decorView.post { updateInjectFrame() }
    }

    override fun onPause() {
        GlassesSessionState.launcherForeground = false
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        GlassesSessionState.launcherForeground = hasFocus
        if (hasFocus) {
            applyImmersiveFullscreen()
            syncSessionDisplayId()
            updateInjectFrame()
        }
    }

    private fun launchApp(appLauncher: AppLauncher, app: LaunchableApp) {
        val displayId = display?.displayId
            ?: GlassesSessionState.secondaryDisplayId
            ?: return
        val now = System.currentTimeMillis()
        if (now - lastLaunchAtMs < LAUNCH_DEBOUNCE_MS) return
        lastLaunchAtMs = now
        Log.d(TAG, "Launching ${app.label} on displayId=$displayId")
        appLauncher.launchOnDisplay(app.componentName, displayId)
        window.decorView.post { moveTaskToBack(true) }
    }

    private fun syncSessionDisplayId() {
        val resolved = DisplayLaunchHelper.resolveSecondaryDisplayId(
            this,
            display?.displayId ?: GlassesSessionState.secondaryDisplayId,
        )
        if (resolved != null) {
            GlassesSessionState.secondaryDisplayId = resolved
        }
    }

    private fun updateInjectFrame() {
        val decor = window.decorView
        val location = IntArray(2)
        decor.getLocationOnScreen(location)
        val frame = LauncherInjectFrame(
            offsetXPx = location[0].toFloat(),
            offsetYPx = location[1].toFloat(),
            widthPx = decor.width.toFloat().coerceAtLeast(1f),
            heightPx = decor.height.toFloat().coerceAtLeast(1f),
        )
        GlassesSessionState.launcherInjectFrame = frame
        if (isDebugBuild()) {
            Log.d(
                TAG,
                "inject frame display=${display?.displayId} offset=(${frame.offsetXPx},${frame.offsetYPx}) " +
                    "size=${frame.widthPx.toInt()}x${frame.heightPx.toInt()} foreground=${GlassesSessionState.launcherForeground}",
            )
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
        private const val LAUNCH_DEBOUNCE_MS = 1_000L
    }

    private fun isDebugBuild(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
