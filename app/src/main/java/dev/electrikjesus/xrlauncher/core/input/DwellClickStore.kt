package dev.electrikjesus.xrlauncher.core.input

import android.content.Context
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cardboard-style dwell click preference. Effective only while FPS mouse-look + glasses IMU
 * are both active; [enabledWanted] is kept when the gate drops so re-entering restores dwell.
 */
object DwellClickStore {
    private const val PREFS_NAME = "launcher_settings"
    private const val KEY_ENABLED = "dwell_click_enabled"
    private const val KEY_DWELL_MS = "dwell_click_ms"
    const val DEFAULT_DWELL_MS = 1000L
    const val MIN_DWELL_MS = 400L
    const val MAX_DWELL_MS = 2500L

    private var loaded = false

    private val _enabledWanted = MutableStateFlow(false)
    val enabledWanted: StateFlow<Boolean> = _enabledWanted.asStateFlow()

    private val _dwellMs = MutableStateFlow(DEFAULT_DWELL_MS)
    val dwellMs: StateFlow<Long> = _dwellMs.asStateFlow()

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _enabledWanted.value = prefs.getBoolean(KEY_ENABLED, false)
        _dwellMs.value = prefs.getLong(KEY_DWELL_MS, DEFAULT_DWELL_MS)
            .coerceIn(MIN_DWELL_MS, MAX_DWELL_MS)
    }

    fun setEnabledWanted(context: Context, enabled: Boolean) {
        _enabledWanted.value = enabled
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun setDwellMs(context: Context, dwellMs: Long) {
        val normalized = dwellMs.coerceIn(MIN_DWELL_MS, MAX_DWELL_MS)
        _dwellMs.value = normalized
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DWELL_MS, normalized)
            .apply()
    }

    /** FPS mouse-look + RayNeo USB IMU head tracking. */
    fun isAllowed(): Boolean =
        GlassesLookMode.effective() == GlassesLookMode.FPS &&
            GlassesSessionState.xrInputMode == GlassesXrInputMode.GLASSES_HEAD_TRACKING

    fun isActive(): Boolean = _enabledWanted.value && isAllowed()
}
