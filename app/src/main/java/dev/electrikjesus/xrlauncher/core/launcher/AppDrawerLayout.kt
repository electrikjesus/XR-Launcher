package dev.electrikjesus.xrlauncher.core.launcher

/** Flat list entries for the glasses app drawer grid (section headers + apps). */
sealed class AppDrawerItem {
    data class SectionHeader(val letter: Char) : AppDrawerItem()
    data class AppEntry(val app: LaunchableApp) : AppDrawerItem()
}

object AppDrawerLayout {
    /** Alphabetical sections when [query] is blank; flat app list when filtering. */
    fun buildItems(apps: List<LaunchableApp>, query: String = ""): List<AppDrawerItem> {
        if (apps.isEmpty()) return emptyList()
        if (query.isNotBlank()) {
            return apps.map { AppDrawerItem.AppEntry(it) }
        }
        return apps
            .groupBy { sectionLetter(it.label) }
            .toSortedMap()
            .flatMap { (letter, group) ->
                listOf(AppDrawerItem.SectionHeader(letter)) +
                    group.map { AppDrawerItem.AppEntry(it) }
            }
    }

    private fun sectionLetter(label: String): Char {
        val first = label.firstOrNull { it.isLetterOrDigit() } ?: '#'
        return first.uppercaseChar()
    }
}
