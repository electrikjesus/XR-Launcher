package dev.electrikjesus.xrlauncher.core.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Session-scoped recents for the glasses Home Space overview. */
object GlassesRecentApps {
    private const val MAX_RECENTS = 6
    private val _recents = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val recentsFlow: StateFlow<List<LaunchableApp>> = _recents.asStateFlow()

    @Synchronized
    fun record(app: LaunchableApp) {
        val next = _recents.value.toMutableList()
        next.removeAll { it.packageName == app.packageName }
        next.add(0, app)
        while (next.size > MAX_RECENTS) {
            next.removeAt(next.lastIndex)
        }
        _recents.value = next
    }

    @Synchronized
    fun list(): List<LaunchableApp> = _recents.value

    @Synchronized
    fun clear() {
        _recents.value = emptyList()
    }

    @Synchronized
    fun seedIfEmpty(apps: List<LaunchableApp>) {
        if (_recents.value.isNotEmpty() || apps.isEmpty()) return
        _recents.value = apps.take(MAX_RECENTS.coerceAtMost(3))
    }
}
