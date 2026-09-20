package dev.electrikjesus.xrlauncher.ui.spatial.gles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import dev.electrikjesus.xrlauncher.core.launcher.AppIconCache
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDeskState
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk

/**
 * Icon (+ optional label) atlas for GLES desk faces.
 *
 * Labeled bitmaps are taller than wide (BumpDesk `1.25` / `1.38` heightMult). The desk mesh
 * [HomeSpaceDesk.labeledIconHalfHeight] must match [LABELED_ASPECT] or round icons look oval.
 */
object DeskIconBitmaps {
    const val ICON_SIZE = 160
    const val LABEL_HEIGHT = 40
    /** Texture height / width for app + label faces (= BumpDesk APP heightMult). */
    const val LABELED_ASPECT = (ICON_SIZE + LABEL_HEIGHT).toFloat() / ICON_SIZE.toFloat()

    private const val APP_PAD = 8

    fun create(context: Context, icon: HomeSpaceDesk.Icon): Bitmap {
        val withLabel = drawsLabel(icon) || icon.kind == HomeSpaceDesk.Kind.PILE_FOLDER
        val width = ICON_SIZE
        val height = if (withLabel) ICON_SIZE + LABEL_HEIGHT else ICON_SIZE
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        when {
            icon.isBacking -> Unit // pick/physics only — no GLES panel
            icon.kind == HomeSpaceDesk.Kind.PAGE_PREV -> drawChevron(canvas, width, ICON_SIZE, left = true)
            icon.kind == HomeSpaceDesk.Kind.PAGE_NEXT -> drawChevron(canvas, width, ICON_SIZE, left = false)
            icon.kind == HomeSpaceDesk.Kind.PAGE -> drawPageDot(canvas, width, ICON_SIZE, icon.label)
            icon.isAppDrawer -> drawAppDrawer(canvas, width, ICON_SIZE)
            icon.isGroupHandle -> drawGroupMoveHandle(canvas, width, ICON_SIZE)
            icon.isPileBacking -> Unit
            icon.kind == HomeSpaceDesk.Kind.PILE_FOLDER ->
                drawFolderPile(canvas, context, icon, width, ICON_SIZE)
            icon.kind == HomeSpaceDesk.Kind.PILE_STACK ->
                drawStackPile(canvas, context, icon, width, ICON_SIZE)
            icon.isWidget -> drawWidgetPlaceholder(canvas, width, ICON_SIZE)
            else -> drawAppIcon(canvas, context, icon.packageName, width, ICON_SIZE)
        }

        if (drawsLabel(icon)) {
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

    fun drawsLabel(icon: HomeSpaceDesk.Icon): Boolean =
        !icon.isBacking &&
            !icon.isWidget &&
            !icon.isGroupHandle &&
            !icon.isPileBacking &&
            !icon.isPileFace &&
            icon.kind != HomeSpaceDesk.Kind.PAGE &&
            icon.kind != HomeSpaceDesk.Kind.PAGE_PREV &&
            icon.kind != HomeSpaceDesk.Kind.PAGE_NEXT

    private fun drawAppIcon(
        canvas: Canvas,
        context: Context,
        packageName: String,
        width: Int,
        iconSize: Int,
    ) {
        val cx = width * 0.5f
        val cy = iconSize * 0.5f
        val radius = (minOf(width, iconSize) * 0.5f) - APP_PAD
        canvas.save()
        canvas.clipPath(
            android.graphics.Path().apply {
                addCircle(cx, cy, radius, android.graphics.Path.Direction.CW)
            },
        )
        val drawable = runCatching { AppIconCache.getIcon(context, packageName) }.getOrNull()
        if (drawable != null) {
            drawable.setBounds(APP_PAD, APP_PAD, width - APP_PAD, iconSize - APP_PAD)
            drawable.draw(canvas)
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 80, 90, 110) }
            canvas.drawCircle(cx, cy, radius, paint)
        }
        canvas.restore()
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

    private fun drawWidgetPlaceholder(canvas: Canvas, width: Int, iconSize: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(200, 50, 58, 72)
        canvas.drawRoundRect(RectF(4f, 4f, width - 4f, iconSize - 4f), 16f, 16f, paint)
        paint.color = Color.argb(220, 180, 190, 210)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 28f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Widget", width / 2f, iconSize / 2f + 10f, paint)
    }

    private fun drawFolderPile(
        canvas: Canvas,
        context: Context,
        icon: HomeSpaceDesk.Icon,
        width: Int,
        iconSize: Int,
    ) {
        val pile = HomeSpaceDeskState.piles.firstOrNull { it.id == icon.componentKey }
        val packages = pile?.members?.map { it.packageName }.orEmpty()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(230, 48, 56, 70)
        canvas.drawRoundRect(RectF(4f, 4f, width - 4f, iconSize - 4f), 28f, 28f, paint)
        val padding = iconSize * 0.14f
        val gap = iconSize * 0.06f
        val cell = (iconSize - 2f * padding - gap) / 2f
        for (index in 0 until 4) {
            val pkg = packages.getOrNull(index) ?: break
            val row = index / 2
            val col = index % 2
            val left = padding + col * (cell + gap)
            val top = padding + row * (cell + gap)
            drawAppIconInRect(canvas, context, pkg, left, top, cell)
        }
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 22f
        paint.typeface = Typeface.DEFAULT_BOLD
        val label = (pile?.name ?: icon.label).take(14)
        canvas.drawText(label, width / 2f, iconSize + LABEL_HEIGHT * 0.72f, paint)
    }

    private fun drawStackPile(
        canvas: Canvas,
        context: Context,
        icon: HomeSpaceDesk.Icon,
        width: Int,
        iconSize: Int,
    ) {
        val pile = HomeSpaceDeskState.piles.firstOrNull { it.id == icon.componentKey }
        val packages = pile?.members?.map { it.packageName }.orEmpty()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Layered plates behind the top icon.
        for (layer in 2 downTo 1) {
            val inset = layer * 6f
            paint.color = Color.argb(160 - layer * 30, 70, 80, 100)
            canvas.drawRoundRect(
                RectF(8f + inset, 8f + inset * 0.5f, width - 8f - inset * 0.3f, iconSize - 8f - inset),
                22f,
                22f,
                paint,
            )
        }
        val topPkg = packages.firstOrNull() ?: icon.packageName
        drawAppIcon(canvas, context, topPkg, width, iconSize)
        paint.color = Color.argb(220, 90, 140, 220)
        canvas.drawCircle(width - 28f, 28f, 18f, paint)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT_BOLD
        val count = (pile?.members?.size ?: 0).coerceAtLeast(1)
        canvas.drawText(count.toString(), width - 28f, 35f, paint)
    }

    private fun drawAppIconInRect(
        canvas: Canvas,
        context: Context,
        packageName: String,
        left: Float,
        top: Float,
        size: Float,
    ) {
        val drawable = runCatching { AppIconCache.getIcon(context, packageName) }.getOrNull()
        if (drawable != null) {
            drawable.setBounds(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
            drawable.draw(canvas)
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 80, 90, 110) }
            canvas.drawRoundRect(RectF(left, top, left + size, top + size), size * 0.2f, size * 0.2f, paint)
        }
    }

    private fun drawGroupMoveHandle(canvas: Canvas, width: Int, iconSize: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = width / 2f
        val cy = iconSize / 2f
        paint.color = Color.argb(210, 70, 130, 220)
        canvas.drawCircle(cx, cy, iconSize * 0.42f, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.strokeCap = Paint.Cap.ROUND
        val arm = iconSize * 0.22f
        canvas.drawLine(cx - arm, cy, cx + arm, cy, paint)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, paint)
        // Arrow tips
        paint.style = Paint.Style.FILL
        val tip = iconSize * 0.08f
        fun arrow(tx: Float, ty: Float, dx: Float, dy: Float) {
            val path = android.graphics.Path()
            path.moveTo(tx, ty)
            path.lineTo(tx - dy * tip - dx * tip, ty + dx * tip - dy * tip)
            path.lineTo(tx + dy * tip - dx * tip, ty - dx * tip - dy * tip)
            path.close()
            canvas.drawPath(path, paint)
        }
        arrow(cx + arm, cy, 1f, 0f)
        arrow(cx - arm, cy, -1f, 0f)
        arrow(cx, cy - arm, 0f, -1f)
        arrow(cx, cy + arm, 0f, 1f)
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
