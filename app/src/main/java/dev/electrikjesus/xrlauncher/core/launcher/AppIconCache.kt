package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * In-memory cache for app icons — avoids repeated PackageManager lookups during grid scroll.
 *
 * Bakes every icon to the same diameter circular face. Adaptive icons keep their OEM round/squircle
 * mask (bounds stay on-canvas). A hard circle clip guarantees transparent corners so GLES/Compose
 * never show a square plate. A mild center zoom equalizes fill without overflowing the round face.
 */
object AppIconCache {
    private const val NORMALIZED_SIZE_PX = 192
    /** Mild zoom so sparse adaptive logos fill the round face like denser ones. */
    private const val FILL_ZOOM = 1.12f
    private val cache = mutableMapOf<String, Drawable>()

    fun getIcon(context: Context, packageName: String): Drawable {
        return cache.getOrPut(packageName) {
            val raw = context.packageManager.getApplicationIcon(packageName)
            BitmapDrawable(context.resources, normalizeToBitmap(raw, NORMALIZED_SIZE_PX))
        }
    }

    /**
     * Round-face bake. Do **not** expand [AdaptiveIconDrawable] past the canvas — that enlarges
     * the system mask beyond the bitmap and fills a square box (the regression users saw).
     */
    internal fun normalizeToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx * 0.5f
        val cy = sizePx * 0.5f
        val radius = sizePx * 0.5f
        val clip = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clip)
        canvas.scale(FILL_ZOOM, FILL_ZOOM, cx, cy)

        val icon = drawable.mutate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
            // On-canvas bounds → OEM round/squircle mask stays round inside the clip.
            icon.setBounds(0, 0, sizePx, sizePx)
        } else {
            val iw = icon.intrinsicWidth.takeIf { it > 0 } ?: sizePx
            val ih = icon.intrinsicHeight.takeIf { it > 0 } ?: sizePx
            // Center-crop into the circle so legacy art shares the same diameter.
            val scale = max(sizePx.toFloat() / iw, sizePx.toFloat() / ih)
            val dw = (iw * scale).roundToInt().coerceAtLeast(1)
            val dh = (ih * scale).roundToInt().coerceAtLeast(1)
            val left = (sizePx - dw) / 2
            val top = (sizePx - dh) / 2
            icon.setBounds(left, top, left + dw, top + dh)
        }
        icon.draw(canvas)
        canvas.restore()
        return bitmap
    }

    internal fun clear() {
        cache.clear()
    }
}
