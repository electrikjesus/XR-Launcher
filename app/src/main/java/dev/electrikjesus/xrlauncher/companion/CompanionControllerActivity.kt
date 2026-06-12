package dev.electrikjesus.xrlauncher.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.MotionPointerController
import dev.electrikjesus.xrlauncher.ui.companion.CompanionTouchpadScreen
import dev.electrikjesus.xrlauncher.ui.theme.XRLauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CompanionControllerActivity : ComponentActivity() {
    private lateinit var motionController: MotionPointerController
    private var isCalibrating by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        motionController = MotionPointerController(this) { deltaX, deltaY ->
            if (CompanionPointerBus.motionControlEnabled.value) {
                CompanionPointerBus.moveByMotion(deltaX, deltaY)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CompanionPointerBus.motionControlEnabled.collect { enabled ->
                    if (enabled) {
                        motionController.start()
                        runCalibration()
                    } else {
                        motionController.stop()
                    }
                }
            }
        }

        setContent {
            XRLauncherTheme {
                CompanionTouchpadScreen(
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
        super.onDestroy()
    }

    companion object {
        private const val CALIBRATION_MS = 500L
    }
}
