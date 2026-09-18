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
import dev.electrikjesus.xrlauncher.core.display.SessionWake
import dev.electrikjesus.xrlauncher.core.display.LauncherInjectFrame
import dev.electrikjesus.xrlauncher.core.display.SubspaceSpike
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsGridConfigStore
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.GlassesRecentApps
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.launcher.PanelEmbedRegistry
import dev.electrikjesus.xrlauncher.core.launcher.PanelLaunchResult
import dev.electrikjesus.xrlauncher.core.launcher.WorkspaceAppLaunchCoordinator
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import dev.electrikjesus.xrlauncher.ui.external.ExternalDisplayWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme
import kotlinx.coroutines.launch

class ExternalDisplayActivity : ComponentActivity() {
    private var lastLaunchAtMs = 0L
    private lateinit var launchCoordinator: WorkspaceAppLaunchCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLauncher = AppLauncher(this)
        val workspaceRepository = WorkspaceRepository(applicationContext)
        val embedRegistry = PanelEmbedRegistry.fromActivity(this)
        launchCoordinator = WorkspaceAppLaunchCoordinator(
            activity = this,
            appLauncher = appLauncher,
            workspaceRepository = workspaceRepository,
            embedRegistry = embedRegistry,
        )
        GlassesSessionState.panelEmbedRegistry = embedRegistry
        applyImmersiveFullscreen()
        syncSessionDisplayId()
        AllAppsGridConfigStore.init(this)
        GlassesSessionState.onLauncherRootSized = { window.decorView.post { updateInjectFrame() } }

        val displayId = display?.displayId ?: Display.DEFAULT_DISPLAY
        val subspaceDecision = SubspaceSpike.resolvePreferSubspace(this)
        GlassesSessionState.subspaceDecision = subspaceDecision
        if (!GlassesSessionState.preferSubspaceShell && subspaceDecision.preferSubspaceShell) {
            GlassesSessionState.preferSubspaceShell = true
        }
        if (isDebugBuild()) {
            Log.d(
                TAG,
                "onCreate displayId=$displayId saved=${savedInstanceState != null} " +
                    "subspace=${GlassesSessionState.preferSubspaceShell} " +
                    "spatialApi=${subspaceDecision.hasSpatialApi} forced=${subspaceDecision.forcedForSpike}",
            )
        }

        setContent {
            val scope = rememberCoroutineScope()
            val repo = remember { WorkspaceRepository(applicationContext) }
            XRLauncherTheme(forGlasses = true) {
                ExternalDisplayWorkspaceScreen(
                    launcherPackageName = packageName,
                    workspaceRepository = repo,
                    onLaunchApp = { app -> launchApp(app) },
                    onCloseEmbedded = { panelId -> launchCoordinator.closeEmbedded(panelId) },
                    onPopOutEmbedded = { panelId ->
                        val displayId = display?.displayId
                            ?: GlassesSessionState.secondaryDisplayId
                            ?: return@ExternalDisplayWorkspaceScreen
                        launchCoordinator.popOutEmbedded(panelId, displayId)
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

    override fun onStart() {
        super.onStart()
        GlassesSessionState.markLauncherForeground()
    }

    override fun onResume() {
        super.onResume()
        GlassesSessionState.markLauncherForeground()
        syncSessionDisplayId()
        window.decorView.post {
            updateInjectFrame()
            consumePendingAppLaunch()
        }
        if (isDebugBuild()) {
            Log.i(
                SubspaceSpike.TAG,
                "onResume subspace=${GlassesSessionState.preferSubspaceShell} " +
                    "outerComposed=${GlassesSessionState.subspaceOuterComposed} " +
                    "innerComposed=${GlassesSessionState.subspaceInnerComposed} " +
                    "displayId=${display?.displayId}",
            )
        }
    }

    override fun onStop() {
        // Do not clear launcherForeground here — on dual-display, the phone companion can
        // take focus (onStop) while the launcher stays visible on the glasses display.
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen()
            syncSessionDisplayId()
            updateInjectFrame()
        }
    }

    private fun consumePendingAppLaunch() {
        val pending = GlassesSessionState.consumePendingAppLaunch() ?: return
        val app = pending.toLaunchableApp() ?: return
        GlassesRecentApps.record(app)
        launchApp(app, preferEmbedded = pending.preferEmbedded)
    }

    private fun launchApp(app: LaunchableApp, preferEmbedded: Boolean = true) {
        val displayId = display?.displayId
            ?: GlassesSessionState.secondaryDisplayId
            ?: return
        val now = System.currentTimeMillis()
        if (now - lastLaunchAtMs < LAUNCH_DEBOUNCE_MS) return
        lastLaunchAtMs = now
        Log.d(TAG, "Launching ${app.label} on displayId=$displayId")
        val result = launchCoordinator.launchFromGlasses(
            app = app,
            displayId = displayId,
            moveLauncherToBack = {
                GlassesSessionState.markLauncherBackgrounded()
                window.decorView.post { moveTaskToBack(true) }
            },
            preferEmbedded = preferEmbedded,
        )
        if (result is PanelLaunchResult.Embedded) {
            GlassesSessionState.markLauncherForeground()
        }
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

    override fun onDestroy() {
        GlassesSessionState.onLauncherRootSized = null
        GlassesSessionState.clearLauncherSession()
        launchCoordinator.embedRegistry?.disposeAll()
        GlassesSessionState.panelEmbedRegistry = null
        super.onDestroy()
    }

    private fun applyImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        SessionWake.keepDisplayOn(this)
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
