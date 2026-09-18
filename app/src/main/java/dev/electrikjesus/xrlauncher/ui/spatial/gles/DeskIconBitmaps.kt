package dev.electrikjesus.xrlauncher.ui.spatial.gles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import dev.electrikjesus.xrlauncher.core.launcher.AppIconCache
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk

/** Icon + label atlas for GLES desk boxes (BumpDesk combined-bitmap layout). */
object DeskIconBitmaps {
    private const val ICON_SIZE = 160
    private const val LABEL_HEIGHT = 40

    fun create(context: Context, icon: HomeSpaceDesk.Icon): Bitmap {
        val width = ICON_SIZE
        val height = ICON_SIZE + LABEL_HEIGHT
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val drawable = runCatching { AppIconCache.getIcon(context, icon.packageName) }.getOrNull()
        if (drawable != null) {
            val pad = 8
            drawable.setBounds(pad, pad, width - pad, ICON_SIZE - pad)
            drawable.draw(canvas)
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 80, 90, 110) }
            canvas.drawRoundRect(RectF(12f, 12f, width - 12f, ICON_SIZE - 12f), 24f, 24f, paint)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val label = icon.label.take(16)
        val textY = ICON_SIZE + LABEL_HEIGHT * 0.72f
        canvas.drawText(label, width / 2f, textY, textPaint)
        return bitmap
    }
}
