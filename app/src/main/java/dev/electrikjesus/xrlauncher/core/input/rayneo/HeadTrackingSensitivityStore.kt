package dev.electrikjesus.xrlauncher.core.input.rayneo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HeadTrackingMovementScales(
    val yawScale: Float,
    val pitchScale: Float,
) {
    companion object {
        fun defaults(): HeadTrackingMovementScales = HeadTrackingMovementScales(
            yawScale = HeadTrackingSensitivityStore.DEFAULT,
            pitchScale = HeadTrackingSensitivityStore.DEFAULT,
        )
    }
}

/** Persists companion head-tracking movement scale per axis (mouse-look gain). */
object HeadTrackingSensitivityStore {
    private const val PREFS_NAME = "head_tracking_calibration"
    private const val KEY_SENSITIVITY_LEGACY = "movement_scale"
    private const val KEY_YAW_SCALE = "movement_scale_yaw"
    private const val KEY_PITCH_SCALE = "movement_scale_pitch"

    const val DEFAULT = 0.45f
    const val MIN = 0.08f
    const val MAX = 2f

    private var appContext: Context? = null
    private var loaded = false

    private val _movementScales = MutableStateFlow(HeadTrackingMovementScales.defaults())
    val movementScales: StateFlow<HeadTrackingMovementScales> = _movementScales.asStateFlow()

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        appContext = context.applicationContext
        _movementScales.value = load()
    }

    fun current(): HeadTrackingMovementScales = _movementScales.value

    fun load(): HeadTrackingMovementScales {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?: return HeadTrackingMovementScales.defaults()
        val legacy = prefs.getFloat(KEY_SENSITIVITY_LEGACY, -1f)
        val legacyDefault = if (legacy >= 0f) legacy.coerceIn(MIN, MAX) else DEFAULT
        return HeadTrackingMovementScales(
            yawScale = prefs.getFloat(KEY_YAW_SCALE, legacyDefault).coerceIn(MIN, MAX),
            pitchScale = prefs.getFloat(KEY_PITCH_SCALE, legacyDefault).coerceIn(MIN, MAX),
        )
    }

    fun saveYaw(value: Float) {
        save(current().copy(yawScale = value.coerceIn(MIN, MAX)))
    }

    fun savePitch(value: Float) {
        save(current().copy(pitchScale = value.coerceIn(MIN, MAX)))
    }

    fun resetToDefaults() {
        save(HeadTrackingMovementScales.defaults())
    }

    private fun save(scales: HeadTrackingMovementScales) {
        _movementScales.value = scales
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putFloat(KEY_YAW_SCALE, scales.yawScale)
            ?.putFloat(KEY_PITCH_SCALE, scales.pitchScale)
            ?.apply()
    }
}
