package dev.electrikjesus.xrlauncher.core.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MotionPointerController(
    context: Context,
    private val onMotionDelta: (deltaX: Float, deltaY: Float) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var biasX = 0f
    private var biasY = 0f
    private var calibrating = false
    private val calibrationSamples = mutableListOf<FloatArray>()

    fun start() {
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    val isAvailable: Boolean get() = gyroscope != null

    fun beginCalibration() {
        calibrationSamples.clear()
        calibrating = true
    }

    fun endCalibration() {
        calibrating = false
        if (calibrationSamples.isNotEmpty()) {
            biasX = calibrationSamples.map { it[0] }.average().toFloat()
            biasY = calibrationSamples.map { it[1] }.average().toFloat()
        }
        calibrationSamples.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        if (calibrating) {
            calibrationSamples.add(floatArrayOf(event.values[0], event.values[1]))
            return
        }
        val adjustedX = event.values[0] - biasX
        val adjustedY = event.values[1] - biasY
        // Y rotation → horizontal cursor; inverted X rotation → vertical (laser-pointer aim).
        onMotionDelta(-adjustedY, -adjustedX)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
