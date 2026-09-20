package dev.electrikjesus.xrlauncher.core.workspace

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.SizeF
import android.view.View
import kotlin.math.max
import kotlin.math.roundToInt

/** Sizing / capture helpers for desk AppWidgets (BumpDesk WidgetUtils subset). */
object DeskWidgetUtils {
    private const val MIN_CAPTURE_PX = 320
    private const val MAX_CAPTURE_PX = 2048
    private const val LAUNCHER_CELL_DP = 70f
    /** World half-width for a ~4-cell-wide widget at default icon scale. */
    private const val REFERENCE_HALF_WIDTH = 0.55f

    fun widgetKey(appWidgetId: Int): String = "widget_$appWidgetId"

    fun parseWidgetId(componentKey: String): Int? =
        componentKey.removePrefix("widget_").toIntOrNull()
            ?.takeIf { componentKey.startsWith("widget_") }

    fun aspectRatioFromProvider(info: AppWidgetProviderInfo): Float {
        val cellDp = dpPerCell(info)
        val w = defaultWidthDp(info, cellDp).coerceAtLeast(1)
        val h = defaultHeightDp(info, cellDp).coerceAtLeast(1)
        return (w.toFloat() / h.toFloat()).coerceIn(0.35f, 8f)
    }

    fun defaultHalfExtents(info: AppWidgetProviderInfo): Pair<Float, Float> {
        val aspect = aspectRatioFromProvider(info)
        val halfW = REFERENCE_HALF_WIDTH * (defaultWidthDp(info, dpPerCell(info)) / (4f * LAUNCHER_CELL_DP))
            .coerceIn(0.55f, 1.8f)
        val halfH = (halfW / aspect).coerceIn(0.12f, 1.4f)
        return halfW to halfH
    }

    fun captureSizePx(
        context: Context,
        info: AppWidgetProviderInfo,
        halfWidth: Float,
        halfHeight: Float,
    ): Pair<Int, Int> {
        val cellDp = dpPerCell(info)
        val defaultW = defaultWidthDp(info, cellDp)
        val widthDp = (halfWidth / REFERENCE_HALF_WIDTH * defaultW).roundToInt()
            .coerceAtLeast(info.minWidth)
        val heightDp = (halfHeight / REFERENCE_HALF_WIDTH * defaultW).roundToInt()
            .coerceAtLeast(info.minHeight)
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).roundToInt().coerceIn(MIN_CAPTURE_PX, MAX_CAPTURE_PX)
        val heightPx = (heightDp * density).roundToInt().coerceIn(MIN_CAPTURE_PX, MAX_CAPTURE_PX)
        return widthPx to heightPx
    }

    fun configureHostView(
        hostView: AppWidgetHostView,
        context: Context,
        info: AppWidgetProviderInfo,
        appWidgetId: Int,
        halfWidth: Float,
        halfHeight: Float,
    ) {
        val (widthPx, heightPx) = captureSizePx(context, info, halfWidth, halfHeight)
        val density = context.resources.displayMetrics.density
        val widthDp = max((widthPx / density).roundToInt(), info.minWidth)
        val heightDp = max((heightPx / density).roundToInt(), info.minHeight)

        hostView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        hostView.setPadding(0, 0, 0, 0)
        hostView.translationX = appWidgetId * 4096f
        hostView.translationY = 0f

        if (WidgetCaptureCoordinator.shouldUpdateAppWidgetSize(appWidgetId, widthDp, heightDp)) {
            val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            val newOptions = options ?: Bundle()
            hostView.updateAppWidgetSize(
                newOptions,
                listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())),
            )
        }

        if (hostView.width != widthPx || hostView.height != heightPx) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            hostView.measure(widthSpec, heightSpec)
            hostView.layout(0, 0, widthPx, heightPx)
        }
    }

    fun captureBitmap(hostView: AppWidgetHostView): Bitmap? {
        val w = hostView.width
        val h = hostView.height
        if (w <= 0 || h <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            hostView.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun dpPerCell(info: AppWidgetProviderInfo): Float {
        if (info.targetCellWidth > 0 && info.minWidth > 0) {
            return (info.minWidth + 30f) / info.targetCellWidth
        }
        return LAUNCHER_CELL_DP
    }

    private fun defaultWidthDp(info: AppWidgetProviderInfo, cellDp: Float): Int {
        if (info.targetCellWidth > 0) {
            return (info.targetCellWidth * cellDp).roundToInt().coerceAtLeast(info.minWidth)
        }
        return info.minWidth
    }

    private fun defaultHeightDp(info: AppWidgetProviderInfo, cellDp: Float): Int {
        if (info.targetCellHeight > 0) {
            return (info.targetCellHeight * cellDp).roundToInt().coerceAtLeast(info.minHeight)
        }
        if (info.minResizeHeight > info.minHeight) return info.minResizeHeight
        if (info.minWidth > info.minHeight * 3) {
            val wideDefault = (info.minWidth / 2.5f).roundToInt()
            val cellDefault = (2f * cellDp).roundToInt()
            return maxOf(cellDefault, wideDefault).coerceAtLeast(info.minHeight)
        }
        return info.minHeight
    }
}
