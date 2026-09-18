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

        when {
            icon.isBacking -> drawBacking(canvas, width, ICON_SIZE)
            icon.kind == HomeSpaceDesk.Kind.PAGE_PREV -> drawChevron(canvas, width, ICON_SIZE, left = true)
            icon.kind == HomeSpaceDesk.Kind.PAGE_NEXT -> drawChevron(canvas, width, ICON_SIZE, left = false)
            icon.kind == HomeSpaceDesk.Kind.PAGE -> drawPageDot(canvas, width, ICON_SIZE, icon.label)
            icon.isAppDrawer -> drawAppDrawer(canvas, width, ICON_SIZE)
            else -> {
                val drawable = runCatching { AppIconCache.getIcon(context, icon.packageName) }.getOrNull()
                if (drawable != null) {
                    val pad = 8
                    drawable.setBounds(pad, pad, width - pad, ICON_SIZE - pad)
                    drawable.draw(canvas)
                } else {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 80, 90, 110) }
                    canvas.drawRoundRect(RectF(12f, 12f, width - 12f, ICON_SIZE - 12f), 24f, 24f, paint)
                }
            }
        }

        if (!icon.isBacking &&
            icon.kind != HomeSpaceDesk.Kind.PAGE &&
            icon.kind != HomeSpaceDesk.Kind.PAGE_PREV &&
            icon.kind != HomeSpaceDesk.Kind.PAGE_NEXT
        ) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val label = icon.label.take(16)
            val textY = ICON_SIZE + LABEL_HEIGHT * 0.72f
            canvas.drawText(label, width / 2f, textY, textPaint)
        }
        return bitmap
    }

    private fun drawBacking(canvas: Canvas, width: Int, iconSize: Int) {
        // Intentionally blank — open-drawer backing is pick/physics only (no GLES panel).
    }

    private fun drawChevron(canvas: Canvas, width: Int, iconSize: Int, left: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 230, 236, 248) }
        val cx = width / 2f
        val cy = iconSize / 2f
        val arm = iconSize * 0.22f
        val path = android.graphics.Path()
        if (left) {
            path.moveTo(cx + arm * 0.4f, cy - arm)
            path.lineTo(cx - arm * 0.6f, cy)
            path.lineTo(cx + arm * 0.4f, cy + arm)
        } else {
            path.moveTo(cx - arm * 0.4f, cy - arm)
            path.lineTo(cx + arm * 0.6f, cy)
            path.lineTo(cx - arm * 0.4f, cy + arm)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(path, paint)
    }

    private fun drawPageDot(canvas: Canvas, width: Int, iconSize: Int, label: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 138, 180, 248) }
        val cx = width / 2f
        val cy = iconSize / 2f
        canvas.drawCircle(cx, cy, iconSize * 0.22f, paint)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 36f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(label, cx, cy + 13f, paint)
    }

    private fun drawAppDrawer(canvas: Canvas, width: Int, iconSize: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(180, 90, 140, 220)
        canvas.drawRoundRect(
            RectF(8f, 8f, width - 8f, iconSize - 8f),
            28f,
            28f,
            paint,
        )
        paint.color = Color.WHITE
        val padding = iconSize * 0.22f
        val cell = (iconSize - 2f * padding) / 3f
        val dot = cell * 0.58f
        val offset = (cell - dot) / 2f
        for (col in 0..2) {
            for (row in 0..2) {
                val left = padding + col * cell + offset
                val top = padding + row * cell + offset
                canvas.drawRoundRect(RectF(left, top, left + dot, top + dot), dot * 0.3f, dot * 0.3f, paint)
            }
        }
    }
}
