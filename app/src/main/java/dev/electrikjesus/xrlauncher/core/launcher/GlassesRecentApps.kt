package dev.electrikjesus.xrlauncher.core.launcher

/** Session-scoped recents for the glasses Home Space overview. */
object GlassesRecentApps {
    private const val MAX_RECENTS = 6
    private val recents = ArrayDeque<LaunchableApp>()

    @Synchronized
    fun record(app: LaunchableApp) {
        recents.removeAll { it.packageName == app.packageName }
        recents.addFirst(app)
        while (recents.size > MAX_RECENTS) {
            recents.removeLast()
        }
    }

    @Synchronized
    fun list(): List<LaunchableApp> = recents.toList()

    @Synchronized
    fun clear() {
        recents.clear()
    }

    @Synchronized
    fun seedIfEmpty(apps: List<LaunchableApp>) {
        if (recents.isNotEmpty() || apps.isEmpty()) return
        apps.take(MAX_RECENTS.coerceAtMost(3)).forEach { recents.addLast(it) }
    }
}
