package dev.electrikjesus.xrlauncher.core.onboarding

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import dev.electrikjesus.xrlauncher.accessibility.DisplayPointerAccessibilityService
import dev.electrikjesus.xrlauncher.core.input.DisplayPointerInjector
import dev.electrikjesus.xrlauncher.notifications.TrayNotificationListenerService

object OnboardingPermissions {
    fun snapshot(context: Context): OnboardingGrantState = OnboardingGrantState(
        accessibilityEnabled = isAccessibilityEnabled(context),
        isDefaultHome = isDefaultHome(context),
        notificationListenerEnabled = TrayNotificationListenerService.isEnabled(context),
    )

    fun isAccessibilityEnabled(context: Context): Boolean {
        if (DisplayPointerInjector.isAvailable) return true
        val component = ComponentName(context, DisplayPointerAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return OnboardingLogic.isAccessibilityListed(enabled, component)
    }

    fun isDefaultHome(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = context.packageManager.resolveActivity(
            home,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolve?.activityInfo?.packageName == context.packageName
    }

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** App Info — user enables Restricted settings here before the accessibility toggle appears. */
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun notificationListenerSettingsIntent(): Intent =
        TrayNotificationListenerService.settingsIntent()

    fun requestHomeIntent(context: Context): Intent {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
