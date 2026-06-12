package dev.electrikjesus.xrlauncher.core.input.rayneo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadTrackingCalibrationTest {
    @Test
    fun defaultMapping_matchesLegacyGyroToCursor() {
        val cal = HeadTrackingCalibration.default()
        val (yaw, pitch) = cal.mapGyroRates(gyroXDps = 10f, gyroYDps = 20f, gyroZDps = 0f)
        assertEquals(-20f, yaw, 0.001f)
        assertEquals(-10f, pitch, 0.001f)
    }

    @Test
    fun motionAccumulator_detectsDominantAxis() {
        val acc = HeadTrackingMotionAccumulator()
        acc.reset()
        repeat(30) {
            acc.feed(gyroXDps = 2f, gyroYDps = 40f, gyroZDps = 1f)
        }
        val capture = acc.captureOrNull()
        assertNotNull(capture)
        assertEquals(1, capture!!.axis)
        assertTrue(capture.meanSigned > 0f)
    }

    @Test
    fun buildCalibration_assignsPitchYawAndRollAxes() {
        val cal = HeadTrackingCalibrationMath.build(
            lookUp = capture(axis = 0, mean = 25f),
            lookDown = capture(axis = 0, mean = -22f),
            lookLeft = capture(axis = 1, mean = 30f),
            lookRight = capture(axis = 1, mean = -28f),
            tiltLeft = capture(axis = 2, mean = 18f),
            tiltRight = capture(axis = 2, mean = -16f),
        )
        assertTrue(cal.isCalibrated)
        assertEquals(0, cal.pitchAxis)
        assertEquals(-1f, cal.pitchSign, 0.001f)
        assertEquals(1, cal.yawAxis)
        assertEquals(-1f, cal.yawSign, 0.001f)
        assertEquals(2, cal.rollAxis)
    }

    private fun capture(axis: Int, mean: Float): HeadTrackingStepCapture =
        HeadTrackingStepCapture(axis = axis, meanSigned = mean, magnitude = 500f)
}
