package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Size of the bottom-corner "XR" circle that returns to the glasses launcher
 * while another app is in front (accessibility overlay).
 */
object LauncherReturnBubbleStore {
    private const val PREFS_NAME = "launcher_settings"
    private const val KEY_SIZE_DP = "return_bubble_size_dp"

    const val MIN_SIZE_DP = 48f
    const val MAX_SIZE_DP = 128f
    /** Larger than the original 56dp hard-code — easier to hit on glasses. */
    const val DEFAULT_SIZE_DP = 80f

    private var loaded = false

    private val _sizeDp = MutableStateFlow(DEFAULT_SIZE_DP)
    val sizeDp: StateFlow<Float> = _sizeDp.asStateFlow()

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        _sizeDp.value = load(context.applicationContext)
    }

    fun current(): Float = _sizeDp.value

    fun load(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_SIZE_DP, DEFAULT_SIZE_DP)
            .coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
    }

    fun saveSizeDp(context: Context, sizeDp: Float) {
        val normalized = sizeDp.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
        _sizeDp.value = normalized
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_SIZE_DP, normalized)
            .apply()
    }

    fun resetToDefaults(context: Context) {
        saveSizeDp(context, DEFAULT_SIZE_DP)
    }
}
