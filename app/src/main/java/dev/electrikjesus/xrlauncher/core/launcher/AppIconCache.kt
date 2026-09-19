package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * In-memory cache for app icons — avoids repeated PackageManager lookups during grid scroll.
 *
 * Icons are normalized to a fixed square bitmap so adaptive-icon safe-zone insets and
 * legacy PNGs with different intrinsic sizes read at the same visual weight on Home and Desktop.
 */
object AppIconCache {
    private const val NORMALIZED_SIZE_PX = 192
    private val cache = mutableMapOf<String, Drawable>()

    fun getIcon(context: Context, packageName: String): Drawable {
        return cache.getOrPut(packageName) {
            val raw = context.packageManager.getApplicationIcon(packageName)
            BitmapDrawable(context.resources, normalizeToBitmap(raw, NORMALIZED_SIZE_PX))
        }
    }

    /**
     * Expand [AdaptiveIconDrawable] by its extra inset so the masked logo fills the square
     * like a legacy launcher icon; non-adaptive drawables are center-cropped to fill.
     */
    internal fun normalizeToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val icon = drawable.mutate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
            val inset = (sizePx * AdaptiveIconDrawable.getExtraInsetFraction()).roundToInt()
            icon.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
        } else {
            val iw = icon.intrinsicWidth.takeIf { it > 0 } ?: sizePx
            val ih = icon.intrinsicHeight.takeIf { it > 0 } ?: sizePx
            val scale = max(sizePx.toFloat() / iw, sizePx.toFloat() / ih)
            val dw = (iw * scale).roundToInt().coerceAtLeast(1)
            val dh = (ih * scale).roundToInt().coerceAtLeast(1)
            val left = (sizePx - dw) / 2
            val top = (sizePx - dh) / 2
            icon.setBounds(left, top, left + dw, top + dh)
        }
        icon.draw(canvas)
        return bitmap
    }

    internal fun clear() {
        cache.clear()
    }
}
