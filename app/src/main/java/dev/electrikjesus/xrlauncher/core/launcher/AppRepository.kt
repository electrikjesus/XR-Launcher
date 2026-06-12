package dev.electrikjesus.xrlauncher.core.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserManager

data class LaunchableApp(
    val label: String,
    val componentName: ComponentName,
    val packageName: String,
)

class AppRepository(private val context: Context) {
    fun loadLaunchableApps(): List<LaunchableApp> {
        val fromLauncherApps = loadViaLauncherApps()
        val fromPackageManager = loadViaPackageManager()
        return (fromLauncherApps + fromPackageManager)
            .distinctBy { "${it.componentName.packageName}/${it.componentName.className}" }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Same approach as desktop launchers ([Taskbar](https://github.com/farmerbb/Taskbar),
     * [Smart Dock](https://github.com/axel358/smartdock)) — enumerate launcher activities
     * per user profile via [LauncherApps].
     */
    private fun loadViaLauncherApps(): List<LaunchableApp> {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        val userManager = context.getSystemService(UserManager::class.java) ?: return emptyList()
        return userManager.userProfiles.flatMap { userHandle ->
            runCatching {
                launcherApps.getActivityList(null, userHandle).map { info ->
                    LaunchableApp(
                        label = info.label?.toString()?.takeIf { it.isNotBlank() }
                            ?: info.componentName.packageName,
                        componentName = info.componentName,
                        packageName = info.componentName.packageName,
                    )
                }
            }.getOrElse { emptyList() }
        }
    }

    private fun loadViaPackageManager(): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos: List<ResolveInfo> = context.packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
        )
        return resolveInfos.mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            LaunchableApp(
                label = info.loadLabel(context.packageManager).toString(),
                componentName = ComponentName(activityInfo.packageName, activityInfo.name),
                packageName = activityInfo.packageName,
            )
        }
    }

    fun filterApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> =
        filterLaunchableApps(apps, query)

    companion object {
        internal fun filterLaunchableApps(
            apps: List<LaunchableApp>,
            query: String,
        ): List<LaunchableApp> {
            if (query.isBlank()) return apps
            val normalized = query.trim().lowercase()
            return apps.filter {
                it.label.lowercase().contains(normalized) ||
                    it.packageName.lowercase().contains(normalized)
            }
        }
    }
}
