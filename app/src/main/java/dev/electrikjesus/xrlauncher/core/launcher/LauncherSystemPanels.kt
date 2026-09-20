package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

/** Curated QS / system panel launches — no SystemUI privileges. */
object LauncherSystemPanels {
    fun openWifi(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        start(context, intent)
    }

    fun openBluetooth(context: Context) {
        start(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun openDisplay(context: Context) {
        start(context, Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }

    fun openNotificationListenerSettings(context: Context) {
        start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openAppNotificationSettings(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        start(context, intent)
    }

    fun setBrightnessFraction(context: Context, fraction: Float): Boolean {
        if (!Settings.System.canWrite(context)) return false
        val value = (fraction.coerceIn(0f, 1f) * 255f).toInt().coerceIn(1, 255)
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value,
            )
        }.getOrDefault(false)
    }

    fun requestWriteSettings(context: Context) {
        if (Settings.System.canWrite(context)) return
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
        start(context, intent)
    }

    private fun start(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
