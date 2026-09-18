package dev.electrikjesus.xrlauncher.core.launcher

import dev.electrikjesus.xrlauncher.core.workspace.HotseatResolver

object PhoneHomeLayout {
    const val HOTSEAT_SLOTS = 4

    fun gridApps(
        apps: List<LaunchableApp>,
        hotseatApps: List<LaunchableApp>,
        query: String,
    ): List<LaunchableApp> {
        val hotseatPackages = hotseatApps.map { it.packageName }.toSet()
        val withoutDock = apps.filter { it.packageName !in hotseatPackages }
        return AppRepository.filterLaunchableApps(withoutDock, query)
    }

    fun resolveHotseat(apps: List<LaunchableApp>, pinnedKeys: List<String>): List<LaunchableApp> =
        HotseatResolver.resolveHotseatApps(apps, pinnedKeys, maxSlots = HOTSEAT_SLOTS)
}
