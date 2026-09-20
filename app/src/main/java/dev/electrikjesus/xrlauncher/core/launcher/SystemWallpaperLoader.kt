package dev.electrikjesus.xrlauncher.core.launcher

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object SystemWallpaperLoader {
    private const val TAG = "SystemWallpaperLoader"

    /** True when [WallpaperManager.getDrawable] / file APIs can return the real image. */
    fun canReadSystemWallpaper(context: Context): Boolean =
        Environment.isExternalStorageManager()

    fun allFilesAccessSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Load the phone home-screen wallpaper as a bitmap suitable for GLES upload. */
    fun loadBitmap(context: Context): Bitmap {
        val app = context.applicationContext
        val wallpaperManager = WallpaperManager.getInstance(app)
        resolveSystemBitmap(wallpaperManager)?.let { return it }
        resolveWallpaperColorsBitmap(wallpaperManager)?.let { return it }
        Log.w(TAG, "system wallpaper unavailable; using twilight fallback")
        return createFallbackBitmap()
    }

    /** Compose-friendly bitmap for legacy in-app wallpaper (prefer GLES cylinder path). */
    fun load(context: Context): ImageBitmap? = loadBitmap(context).asImageBitmap()

    internal fun createFallbackBitmap(): Bitmap =
        createVerticalGradientBitmap(
            top = Color.parseColor("#0D0221"),
            mid = Color.parseColor("#1A0A2E"),
            bottom = Color.parseColor("#05010A"),
        )

    /** Approximate the phone wallpaper from [WallpaperManager.getWallpaperColors] when the image is locked. */
    internal fun createColorsBitmap(
        primary: Int,
        secondary: Int?,
        tertiary: Int?,
    ): Bitmap {
        val mid = secondary ?: blend(primary, Color.BLACK, 0.35f)
        val bottom = tertiary ?: blend(primary, Color.BLACK, 0.7f)
        return createVerticalGradientBitmap(top = primary, mid = mid, bottom = bottom)
    }

    private fun resolveSystemBitmap(wallpaperManager: WallpaperManager): Bitmap? {
        resolveSystemDrawable(wallpaperManager)?.let { drawable ->
            return runCatching { drawableToBitmap(drawable) }
                .onFailure { Log.w(TAG, "drawableToBitmap failed", it) }
                .getOrNull()
        }
        return resolveWallpaperFileBitmap(wallpaperManager)
    }

    private fun resolveSystemDrawable(wallpaperManager: WallpaperManager): Drawable? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM) }
                .onFailure { Log.d(TAG, "getDrawable(FLAG_SYSTEM): ${it.message}") }
                .getOrNull()
                ?.let { return it }
        }
        @Suppress("DEPRECATION")
        runCatching { wallpaperManager.drawable }
            .onFailure { Log.d(TAG, "drawable: ${it.message}") }
            .getOrNull()
            ?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { wallpaperManager.peekDrawable(WallpaperManager.FLAG_SYSTEM) }
                .onFailure { Log.d(TAG, "peekDrawable: ${it.message}") }
                .getOrNull()
                ?.let { return it }
        }
        return null
    }

    private fun resolveWallpaperFileBitmap(wallpaperManager: WallpaperManager): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        var pfd: ParcelFileDescriptor? = null
        return try {
            pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
            val fd = pfd?.fileDescriptor ?: return null
            BitmapFactory.decodeFileDescriptor(fd)
        } catch (t: Throwable) {
            Log.d(TAG, "getWallpaperFile: ${t.message}")
            null
        } finally {
            runCatching { pfd?.close() }
        }
    }

    private fun resolveWallpaperColorsBitmap(wallpaperManager: WallpaperManager): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
        return runCatching {
            val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?: return null
            val primary = colors.primaryColor.toArgb()
            val secondary = colors.secondaryColor?.toArgb()
            val tertiary = colors.tertiaryColor?.toArgb()
            Log.i(TAG, "using WallpaperColors fallback primary=#${Integer.toHexString(primary)}")
            createColorsBitmap(primary, secondary, tertiary)
        }.onFailure {
            Log.d(TAG, "getWallpaperColors: ${it.message}")
        }.getOrNull()
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

    private fun createVerticalGradientBitmap(top: Int, mid: Int, bottom: Int): Bitmap {
        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf(top, mid, bottom),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun blend(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r, g, b)
    }
}
