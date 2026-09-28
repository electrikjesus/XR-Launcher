package dev.electrikjesus.xrlauncher.core.workspace

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Large-screen Home Space: show left/right arrows that glide to the previous / next pane. */
object HostPaneArrowsStore {
    private const val PREFS_NAME = "launcher_settings"
    private const val KEY_ENABLED = "host_pane_arrows_enabled"

    private var loaded = false

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        _enabled.value = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _enabled.value = enabled
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
