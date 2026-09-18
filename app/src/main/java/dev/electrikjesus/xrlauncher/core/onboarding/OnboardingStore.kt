package dev.electrikjesus.xrlauncher.core.onboarding

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** First-run intro + replay from Settings. SharedPreferences, same pattern as All Apps grid. */
object OnboardingStore {
    private const val PREFS_NAME = "launcher_settings"
    private const val KEY_COMPLETED = "onboarding_completed"
    private const val KEY_REPLAY = "onboarding_replay"

    private var loaded = false
    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = prefs(context)
        _completed.value = prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun isCompleted(): Boolean = _completed.value

    fun markCompleted(context: Context) {
        _completed.value = true
        prefs(context).edit()
            .putBoolean(KEY_COMPLETED, true)
            .putBoolean(KEY_REPLAY, false)
            .apply()
    }

    fun requestReplay(context: Context) {
        prefs(context).edit().putBoolean(KEY_REPLAY, true).apply()
    }

    fun consumeReplay(context: Context): Boolean {
        val prefs = prefs(context)
        val replay = prefs.getBoolean(KEY_REPLAY, false)
        if (replay) {
            prefs.edit().putBoolean(KEY_REPLAY, false).apply()
        }
        return replay
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
