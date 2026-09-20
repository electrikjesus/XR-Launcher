package dev.electrikjesus.xrlauncher.companion

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.core.input.MotionPointerController
import dev.electrikjesus.xrlauncher.core.input.rayneo.HeadTrackingCalibrationStore
import dev.electrikjesus.xrlauncher.core.input.rayneo.RayNeoHeadTrackingController
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsGridConfigStore
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.ui.companion.CompanionTouchpadScreen
import dev.electrikjesus.xrlauncher.ui.launcher.rememberLaunchableApps
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CompanionControllerActivity : ComponentActivity() {
    private lateinit var motionController: MotionPointerController
    private var isCalibrating by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()
        HeadTrackingCalibrationStore.init(this)
        AllAppsGridConfigStore.init(this)
        CompanionPointerBus.initHeadTrackingControls(this)

        motionController = MotionPointerController(this) { deltaX, deltaY ->
            if (CompanionPointerBus.motionControlEnabled.value) {
                CompanionPointerBus.moveByMotion(deltaX, deltaY)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CompanionPointerBus.motionControlEnabled.collect { enabled ->
                    if (enabled && GlassesSessionState.xrInputMode == GlassesXrInputMode.COMPANION) {
                        motionController.start()
                        runCalibration()
                    } else {
                        motionController.stop()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                GlassesSessionState.xrInputModeFlow.collect { mode ->
                    when (mode) {
                        GlassesXrInputMode.GLASSES_HEAD_TRACKING -> {
                            CompanionPointerBus.setMotionControlEnabled(false)
                            CompanionPointerBus.recenterCursor()
                            RayNeoHeadTrackingController.start(this@CompanionControllerActivity)
                        }
                        GlassesXrInputMode.COMPANION -> {
                            RayNeoHeadTrackingController.stop()
                        }
                    }
                }
            }
        }

        setContent {
            val workspaceRepository = remember { WorkspaceRepository(applicationContext) }
            val allAppsOverlayVisible by GlassesSessionState.allAppsOverlayVisibleFlow.collectAsState()
            val launchableApps = rememberLaunchableApps(
                excludePackageName = packageName,
                allAppsOverlayVisible,
            )
            XRLauncherTheme(forCompanion = true) {
                CompanionTouchpadScreen(
                    workspaceRepository = workspaceRepository,
                    apps = launchableApps,
                    onLaunchApp = { app, target ->
                        DisplayLaunchHelper.launchApp(this, app, target)
                    },
                    motionAvailable = motionController.isAvailable,
                    isCalibrating = isCalibrating,
                    onCalibrate = { lifecycleScope.launch { runCalibration() } },
                )
            }
        }
    }

    private suspend fun runCalibration() {
        isCalibrating = true
        motionController.beginCalibration()
        delay(CALIBRATION_MS)
        motionController.endCalibration()
        isCalibrating = false
    }

    override fun onDestroy() {
        motionController.stop()
        RayNeoHeadTrackingController.shutdown(this)
        super.onDestroy()
    }

    companion object {
        private const val CALIBRATION_MS = 500L
    }
}
