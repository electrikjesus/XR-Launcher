package dev.electrikjesus.xrlauncher.core.workspace

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.widget.RemoteViews

/** Host view that notifies when RemoteViews content changes (BumpDesk port). */
class XrAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    var onRemoteViewsApplied: (() -> Unit)? = null
    private val pendingCallbacks = mutableListOf<Runnable>()

    init {
        setWillNotDraw(false)
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        scheduleContentCapture()
    }

    fun scheduleContentCapture() {
        pendingCallbacks.forEach { removeCallbacks(it) }
        pendingCallbacks.clear()
        val immediate = Runnable { onRemoteViewsApplied?.invoke() }
        pendingCallbacks.add(immediate)
        post(immediate)
        val retry = Runnable { onRemoteViewsApplied?.invoke() }
        pendingCallbacks.add(retry)
        postDelayed(retry, 1500L)
    }

    override fun onDetachedFromWindow() {
        pendingCallbacks.forEach { removeCallbacks(it) }
        pendingCallbacks.clear()
        super.onDetachedFromWindow()
    }
}

class XrAppWidgetHost(
    context: Context,
    hostId: Int,
    private val onWidgetChanged: (Int) -> Unit,
) : AppWidgetHost(context, hostId) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo,
    ): AppWidgetHostView {
        return XrAppWidgetHostView(context).apply {
            onRemoteViewsApplied = {
                if (WidgetCaptureCoordinator.shouldInvalidateTexture(appWidgetId)) {
                    onWidgetChanged(appWidgetId)
                }
            }
        }
    }

    override fun onProviderChanged(appWidgetId: Int, appWidget: AppWidgetProviderInfo) {
        super.onProviderChanged(appWidgetId, appWidget)
        if (WidgetCaptureCoordinator.shouldInvalidateTexture(appWidgetId)) {
            onWidgetChanged(appWidgetId)
        }
    }
}
