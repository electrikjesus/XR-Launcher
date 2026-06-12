package dev.electrikjesus.xrlauncher.core.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

data class LaunchableApp(
    val label: String,
    val componentName: ComponentName,
    val packageName: String,
)

class AppRepository(private val context: Context) {
    fun loadLaunchableApps(): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos: List<ResolveInfo> = context.packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
        )
        return resolveInfos
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                val component = ComponentName(activityInfo.packageName, activityInfo.name)
                LaunchableApp(
                    label = info.loadLabel(context.packageManager).toString(),
                    componentName = component,
                    packageName = activityInfo.packageName,
                )
            }
            .distinctBy { it.componentName }
            .sortedBy { it.label.lowercase() }
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
