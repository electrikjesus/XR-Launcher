package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import android.graphics.drawable.Drawable

/** In-memory cache for app icons — avoids repeated PackageManager lookups during grid scroll. */
object AppIconCache {
    private val cache = mutableMapOf<String, Drawable>()

    fun getIcon(context: Context, packageName: String): Drawable {
        return cache.getOrPut(packageName) {
            context.packageManager.getApplicationIcon(packageName)
        }
    }

    internal fun clear() {
        cache.clear()
    }
}
