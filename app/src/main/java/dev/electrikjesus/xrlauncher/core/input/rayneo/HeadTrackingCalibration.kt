package dev.electrikjesus.xrlauncher.core.input.rayneo

/**
 * Maps RayNeo gyro axes to mouse-look cursor pan (yaw → cursor X, pitch → cursor Y).
 * Axis indices: 0 = gyroX, 1 = gyroY, 2 = gyroZ.
 */
data class HeadTrackingCalibration(
    val pitchAxis: Int,
    val pitchSign: Float,
    val yawAxis: Int,
    val yawSign: Float,
    val rollAxis: Int,
    val isCalibrated: Boolean,
    val calibratedAtMs: Long = 0L,
) {
    init {
        require(pitchAxis in 0..2 && yawAxis in 0..2 && rollAxis in 0..2)
    }

    fun mapGyroRates(gyroXDps: Float, gyroYDps: Float, gyroZDps: Float): Pair<Float, Float> {
        val gyro = floatArrayOf(gyroXDps, gyroYDps, gyroZDps)
        val yawRate = gyro[yawAxis] * yawSign
        val pitchRate = gyro[pitchAxis] * pitchSign
        return yawRate to pitchRate
    }

    companion object {
        /** Matches legacy hardcoded mapping until the user completes calibration. */
        fun default(): HeadTrackingCalibration = HeadTrackingCalibration(
            pitchAxis = 0,
            pitchSign = -1f,
            yawAxis = 1,
            yawSign = -1f,
            rollAxis = 2,
            isCalibrated = false,
        )

        fun fromPrefs(
            pitchAxis: Int,
            pitchSign: Float,
            yawAxis: Int,
            yawSign: Float,
            rollAxis: Int,
            calibratedAtMs: Long,
        ): HeadTrackingCalibration = HeadTrackingCalibration(
            pitchAxis = pitchAxis,
            pitchSign = pitchSign,
            yawAxis = yawAxis,
            yawSign = yawSign,
            rollAxis = rollAxis,
            isCalibrated = true,
            calibratedAtMs = calibratedAtMs,
        )
    }
}

/** Result of one calibration movement (e.g. look up). */
data class HeadTrackingStepCapture(
    val axis: Int,
    val meanSigned: Float,
    val magnitude: Float,
) {
    companion object {
        internal const val MIN_SAMPLES = 24
        internal const val MIN_TOTAL_MAGNITUDE = 400f
        internal const val MIN_DOMINANCE = 0.55f
        internal const val MIN_PEAK_DPS = 14f
        internal const val MIN_MEAN_DPS = 10f
        internal const val STEP_SETTLE_MS = 700L
    }
}

internal class HeadTrackingMotionAccumulator {
    private val sumSigned = FloatArray(3)
    private val sumAbs = FloatArray(3)
    private val peakAbs = FloatArray(3)
    private var stepReadyAtMs: Long = 0L
    var sampleCount: Int = 0
        private set

    fun beginStep() {
        reset()
        stepReadyAtMs = System.currentTimeMillis() + HeadTrackingStepCapture.STEP_SETTLE_MS
    }

    fun reset() {
        sumSigned.fill(0f)
        sumAbs.fill(0f)
        peakAbs.fill(0f)
        sampleCount = 0
        stepReadyAtMs = 0L
    }

    fun feed(gyroXDps: Float, gyroYDps: Float, gyroZDps: Float): Boolean {
        if (System.currentTimeMillis() < stepReadyAtMs) return false
        val samples = floatArrayOf(gyroXDps, gyroYDps, gyroZDps)
        for (index in 0..2) {
            sumSigned[index] += samples[index]
            val absRate = kotlin.math.abs(samples[index])
            sumAbs[index] += absRate
            if (absRate > peakAbs[index]) peakAbs[index] = absRate
        }
        sampleCount++
        return true
    }

    fun captureOrNull(): HeadTrackingStepCapture? {
        if (sampleCount < HeadTrackingStepCapture.MIN_SAMPLES) return null
        val total = sumAbs.sum()
        if (total < HeadTrackingStepCapture.MIN_TOTAL_MAGNITUDE) return null
        val axis = sumAbs.indices.maxByOrNull { sumAbs[it] } ?: return null
        if (sumAbs[axis] / total < HeadTrackingStepCapture.MIN_DOMINANCE) return null
        if (peakAbs[axis] < HeadTrackingStepCapture.MIN_PEAK_DPS) return null
        val meanSigned = sumSigned[axis] / sampleCount
        if (kotlin.math.abs(meanSigned) < HeadTrackingStepCapture.MIN_MEAN_DPS) return null
        return HeadTrackingStepCapture(
            axis = axis,
            meanSigned = meanSigned,
            magnitude = sumAbs[axis],
        )
    }
}

object HeadTrackingCalibrationMath {
    fun build(
        lookUp: HeadTrackingStepCapture,
        lookDown: HeadTrackingStepCapture,
        lookLeft: HeadTrackingStepCapture,
        lookRight: HeadTrackingStepCapture,
        tiltLeft: HeadTrackingStepCapture,
        tiltRight: HeadTrackingStepCapture,
    ): HeadTrackingCalibration {
        val pitchAxis = resolvePairAxis(lookUp, lookDown)
        // Look up pans the view upward → cursor Y decreases.
        val pitchSign = if (lookUp.meanSigned > 0f) -1f else 1f

        val yawAxis = resolvePairAxis(lookLeft, lookRight, exclude = pitchAxis)
        // Look left pans the view left → cursor X decreases.
        val leftMean = lookLeft.meanSigned
        val yawSign = if (leftMean > 0f) -1f else 1f

        val rollAxis = resolvePairAxis(tiltLeft, tiltRight, exclude = pitchAxis, alsoExclude = yawAxis)

        return HeadTrackingCalibration(
            pitchAxis = pitchAxis,
            pitchSign = pitchSign,
            yawAxis = yawAxis,
            yawSign = yawSign,
            rollAxis = rollAxis,
            isCalibrated = true,
            calibratedAtMs = System.currentTimeMillis(),
        )
    }

    private fun resolvePairAxis(
        first: HeadTrackingStepCapture,
        second: HeadTrackingStepCapture,
        exclude: Int? = null,
        alsoExclude: Int? = null,
    ): Int {
        if (first.axis == second.axis && first.axis != exclude && first.axis != alsoExclude) {
            return first.axis
        }
        val candidates = listOf(first.axis, second.axis)
            .filter { it != exclude && it != alsoExclude }
        return candidates.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: first.axis
    }
}
