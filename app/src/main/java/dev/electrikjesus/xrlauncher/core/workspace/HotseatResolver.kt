package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

object HotseatResolver {
    /** Suggested defaults when the user has not pinned anything yet (package names). */
    val defaultPackageNames: List<String> = listOf(
        "com.android.settings",
        "com.android.vending",
        "com.google.android.apps.photos",
        "com.android.chrome",
    )

    /**
     * Build hotseat row: pinned entries first (stored order), then filler apps.
     * [pinnedKeys] are component flatten strings or package names from DataStore.
     */
    fun resolveHotseatApps(
        apps: List<LaunchableApp>,
        pinnedKeys: List<String>,
        maxSlots: Int = 5,
    ): List<LaunchableApp> {
        val keys = pinnedKeys.ifEmpty {
            defaultPackageNames.filter { pkg -> apps.any { it.packageName == pkg } }
        }
        val pinned = keys.mapNotNull { key -> apps.find { it.matchesPin(key) } }
            .distinctBy { it.packageName }
        val pinnedPackages = pinned.map { it.packageName }.toSet()
        val filler = apps.filter { it.packageName !in pinnedPackages }
        return (pinned + filler).take(maxSlots)
    }

    private fun LaunchableApp.matchesPin(pin: String): Boolean =
        componentName.flattenToString() == pin || packageName == pin
}

fun LaunchableApp.componentKey(): String = componentName.flattenToString()
