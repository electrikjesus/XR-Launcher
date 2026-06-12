package dev.electrikjesus.xrlauncher.core.input.rayneo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HeadTrackingCalibrationStore {
    private const val PREFS_NAME = "head_tracking_calibration"
    private const val KEY_PITCH_AXIS = "pitch_axis"
    private const val KEY_PITCH_SIGN = "pitch_sign"
    private const val KEY_YAW_AXIS = "yaw_axis"
    private const val KEY_YAW_SIGN = "yaw_sign"
    private const val KEY_ROLL_AXIS = "roll_axis"
    private const val KEY_CALIBRATED_AT = "calibrated_at"

    private val _calibration = MutableStateFlow(HeadTrackingCalibration.default())
    val calibration: StateFlow<HeadTrackingCalibration> = _calibration.asStateFlow()

    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val calibratedAt = prefs.getLong(KEY_CALIBRATED_AT, 0L)
        if (calibratedAt <= 0L) return
        _calibration.value = HeadTrackingCalibration.fromPrefs(
            pitchAxis = prefs.getInt(KEY_PITCH_AXIS, 0),
            pitchSign = prefs.getFloat(KEY_PITCH_SIGN, -1f),
            yawAxis = prefs.getInt(KEY_YAW_AXIS, 1),
            yawSign = prefs.getFloat(KEY_YAW_SIGN, 1f),
            rollAxis = prefs.getInt(KEY_ROLL_AXIS, 2),
            calibratedAtMs = calibratedAt,
        )
    }

    fun current(): HeadTrackingCalibration = _calibration.value

    fun save(context: Context, calibration: HeadTrackingCalibration) {
        _calibration.value = calibration
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PITCH_AXIS, calibration.pitchAxis)
            .putFloat(KEY_PITCH_SIGN, calibration.pitchSign)
            .putInt(KEY_YAW_AXIS, calibration.yawAxis)
            .putFloat(KEY_YAW_SIGN, calibration.yawSign)
            .putInt(KEY_ROLL_AXIS, calibration.rollAxis)
            .putLong(KEY_CALIBRATED_AT, calibration.calibratedAtMs)
            .apply()
    }

    fun clear(context: Context) {
        _calibration.value = HeadTrackingCalibration.default()
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
