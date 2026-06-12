package dev.electrikjesus.xrlauncher.core.input.rayneo

import android.content.Context
import android.util.Log
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HeadTrackingCalibrationStep {
    LOOK_UP,
    LOOK_DOWN,
    LOOK_LEFT,
    LOOK_RIGHT,
    TILT_LEFT,
    TILT_RIGHT,
    COMPLETE,
}

data class HeadTrackingCalibrationUiState(
    val active: Boolean = false,
    val currentStep: HeadTrackingCalibrationStep = HeadTrackingCalibrationStep.LOOK_UP,
    val pitchComplete: Boolean = false,
    val yawComplete: Boolean = false,
    val rollComplete: Boolean = false,
    val motionProgress: Float = 0f,
    val complete: Boolean = false,
)

/**
 * Guided head-tracking calibration: captures gyro axes for pitch, yaw, and roll movements.
 */
object HeadTrackingCalibrationSession {
    private const val TAG = "XRLauncher/HeadTrackCal"

    private val _uiState = MutableStateFlow(HeadTrackingCalibrationUiState())
    val uiState: StateFlow<HeadTrackingCalibrationUiState> = _uiState.asStateFlow()

    private val accumulator = HeadTrackingMotionAccumulator()
    private val captures = mutableMapOf<HeadTrackingCalibrationStep, HeadTrackingStepCapture>()

    private var appContext: Context? = null

    val isActive: Boolean
        get() = _uiState.value.active && _uiState.value.currentStep != HeadTrackingCalibrationStep.COMPLETE

    fun start(context: Context) {
        appContext = context.applicationContext
        captures.clear()
        accumulator.beginStep()
        _uiState.value = HeadTrackingCalibrationUiState(
            active = true,
            currentStep = HeadTrackingCalibrationStep.LOOK_UP,
        )
        Log.i(TAG, "Calibration started")
    }

    fun cancel() {
        captures.clear()
        accumulator.reset()
        appContext = null
        _uiState.value = HeadTrackingCalibrationUiState()
        Log.i(TAG, "Calibration cancelled")
    }

    fun feedSample(gyroXDps: Float, gyroYDps: Float, gyroZDps: Float) {
        val state = _uiState.value
        if (!state.active || state.currentStep == HeadTrackingCalibrationStep.COMPLETE) return

        accumulator.feed(gyroXDps, gyroYDps, gyroZDps)
        val progress = (accumulator.sampleCount / HeadTrackingStepCapture.MIN_SAMPLES.toFloat())
            .coerceIn(0f, 1f)
        _uiState.value = state.copy(motionProgress = progress)

        val capture = accumulator.captureOrNull() ?: return
        completeCurrentStep(capture)
    }

    fun finish(context: Context) {
        val lookUp = captures[HeadTrackingCalibrationStep.LOOK_UP] ?: return
        val lookDown = captures[HeadTrackingCalibrationStep.LOOK_DOWN] ?: return
        val lookLeft = captures[HeadTrackingCalibrationStep.LOOK_LEFT] ?: return
        val lookRight = captures[HeadTrackingCalibrationStep.LOOK_RIGHT] ?: return
        val tiltLeft = captures[HeadTrackingCalibrationStep.TILT_LEFT] ?: return
        val tiltRight = captures[HeadTrackingCalibrationStep.TILT_RIGHT] ?: return

        val calibration = HeadTrackingCalibrationMath.build(
            lookUp = lookUp,
            lookDown = lookDown,
            lookLeft = lookLeft,
            lookRight = lookRight,
            tiltLeft = tiltLeft,
            tiltRight = tiltRight,
        )
        HeadTrackingCalibrationStore.save(context, calibration)
        CompanionPointerBus.recenterHeadLook()
        _uiState.value = HeadTrackingCalibrationUiState(active = false)
        Log.i(
            TAG,
            "Calibration saved pitch=axis${calibration.pitchAxis}×${calibration.pitchSign} " +
                "yaw=axis${calibration.yawAxis}×${calibration.yawSign} " +
                "roll=axis${calibration.rollAxis}",
        )
    }

    private fun completeCurrentStep(capture: HeadTrackingStepCapture) {
        val step = _uiState.value.currentStep
        if (captures.containsKey(step)) return

        captures[step] = capture
        accumulator.beginStep()
        Log.i(TAG, "Captured $step axis=${capture.axis} mean=${capture.meanSigned}")

        val nextStep = when (step) {
            HeadTrackingCalibrationStep.LOOK_UP -> HeadTrackingCalibrationStep.LOOK_DOWN
            HeadTrackingCalibrationStep.LOOK_DOWN -> HeadTrackingCalibrationStep.LOOK_LEFT
            HeadTrackingCalibrationStep.LOOK_LEFT -> HeadTrackingCalibrationStep.LOOK_RIGHT
            HeadTrackingCalibrationStep.LOOK_RIGHT -> HeadTrackingCalibrationStep.TILT_LEFT
            HeadTrackingCalibrationStep.TILT_LEFT -> HeadTrackingCalibrationStep.TILT_RIGHT
            HeadTrackingCalibrationStep.TILT_RIGHT -> HeadTrackingCalibrationStep.COMPLETE
            HeadTrackingCalibrationStep.COMPLETE -> HeadTrackingCalibrationStep.COMPLETE
        }

        val pitchComplete = captures.containsKey(HeadTrackingCalibrationStep.LOOK_DOWN)
        val yawComplete = captures.containsKey(HeadTrackingCalibrationStep.LOOK_RIGHT)
        val rollComplete = captures.containsKey(HeadTrackingCalibrationStep.TILT_RIGHT)

        _uiState.value = _uiState.value.copy(
            currentStep = nextStep,
            pitchComplete = pitchComplete,
            yawComplete = yawComplete,
            rollComplete = rollComplete,
            motionProgress = 0f,
        )

        if (nextStep == HeadTrackingCalibrationStep.COMPLETE) {
            appContext?.let { finish(it) }
        }
    }
}
