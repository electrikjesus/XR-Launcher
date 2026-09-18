package dev.electrikjesus.xrlauncher.core.launcher

import android.content.ComponentName

/** Where the phone HOME / companion launcher should start an app. */
enum class AppLaunchTarget {
    PHONE,
    XR_EMBEDDED,
    XR_FULLSCREEN,
}

data class AppLaunchChoice(
    val target: AppLaunchTarget,
    val enabled: Boolean,
)

object AppLaunchChooser {
    fun choices(hasSecondaryDisplay: Boolean): List<AppLaunchChoice> = listOf(
        AppLaunchChoice(AppLaunchTarget.PHONE, enabled = true),
        AppLaunchChoice(AppLaunchTarget.XR_EMBEDDED, enabled = hasSecondaryDisplay),
        AppLaunchChoice(AppLaunchTarget.XR_FULLSCREEN, enabled = hasSecondaryDisplay),
    )

    fun opensCompanionTouchpad(target: AppLaunchTarget): Boolean =
        target == AppLaunchTarget.XR_EMBEDDED || target == AppLaunchTarget.XR_FULLSCREEN
}

data class PendingGlassesAppLaunch(
    val label: String,
    val packageName: String,
    val componentFlattened: String,
    val preferEmbedded: Boolean,
) {
    fun toLaunchableApp(): LaunchableApp? {
        val component = ComponentName.unflattenFromString(componentFlattened) ?: return null
        return LaunchableApp(
            label = label,
            componentName = component,
            packageName = packageName,
        )
    }

    companion object {
        fun from(app: LaunchableApp, preferEmbedded: Boolean): PendingGlassesAppLaunch =
            PendingGlassesAppLaunch(
                label = app.label,
                packageName = app.packageName,
                componentFlattened = app.componentName.flattenToString(),
                preferEmbedded = preferEmbedded,
            )
    }
}
