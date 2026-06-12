package dev.electrikjesus.xrlauncher.core.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object SystemWallpaperLoader {
    /** Load the phone home-screen wallpaper as a bitmap suitable for GLES upload. */
    fun loadBitmap(context: Context): Bitmap? = runCatching {
        val wallpaperManager = WallpaperManager.getInstance(context.applicationContext)
        val drawable = resolveSystemDrawable(wallpaperManager) ?: return null
        drawableToBitmap(drawable)
    }.getOrNull()

    /** Compose-friendly bitmap for legacy in-app wallpaper (prefer GLES cylinder path). */
    fun load(context: Context): ImageBitmap? = loadBitmap(context)?.asImageBitmap()

    private fun resolveSystemDrawable(wallpaperManager: WallpaperManager): Drawable? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM)?.let { return it }
        }
        @Suppress("DEPRECATION")
        wallpaperManager.drawable?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.peekDrawable(WallpaperManager.FLAG_SYSTEM)?.let { return it }
        }
        return null
    }

    internal fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null && !bitmap.isRecycled) {
                return bitmap
            }
        }
        val bounds = drawable.bounds
        val width = when {
            drawable.intrinsicWidth > 0 -> drawable.intrinsicWidth
            bounds.width() > 0 -> bounds.width()
            else -> 1
        }.coerceIn(1, 4096)
        val height = when {
            drawable.intrinsicHeight > 0 -> drawable.intrinsicHeight
            bounds.height() > 0 -> bounds.height()
            else -> 1
        }.coerceIn(1, 4096)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    internal fun drawableToImageBitmap(drawable: Drawable): ImageBitmap =
        drawableToBitmap(drawable).asImageBitmap()
}
