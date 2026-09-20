package dev.electrikjesus.xrlauncher.core.workspace

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Activity-scoped AppWidgetHost for Home Space desk widgets (BumpDesk path).
 * Off-screen [container] hosts views; [captureBitmap] feeds GLES via DeskIconTextureBus.
 */
object DeskWidgetController {
    private const val HOST_ID = 2048
    private const val LOG_TAG = "XRLauncher/Widget"

    private var appContext: Context? = null
    private var host: XrAppWidgetHost? = null
    private var manager: AppWidgetManager? = null
    private var container: FrameLayout? = null
    private var attachedActivity: Activity? = null

    private var pickLauncher: ActivityResultLauncher<Intent>? = null
    private var configureLauncher: ActivityResultLauncher<Intent>? = null

    private var pendingYawDeg: Float = 0f
    private var pendingPitchDeg: Float = 0f
    private var pendingWidgetId: Int = -1

    private val hostViews = mutableMapOf<Int, AppWidgetHostView>()
    private val dirtyIds = mutableSetOf<Int>()

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    private val _changed = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val changed: SharedFlow<Int> = _changed.asSharedFlow()

    fun bindActivity(
        activity: Activity,
        pick: ActivityResultLauncher<Intent>,
        configure: ActivityResultLauncher<Intent>,
    ) {
        appContext = activity.applicationContext
        pickLauncher = pick
        configureLauncher = configure
        if (host == null) {
            manager = AppWidgetManager.getInstance(activity.applicationContext)
            host = XrAppWidgetHost(activity.applicationContext, HOST_ID) { id ->
                dirtyIds += id
                _changed.tryEmit(id)
            }
        }
        ensureContainer(activity)
        startListening()
        restoreHostViewsForPlaced()
    }

    fun unbindActivity(activity: Activity) {
        if (attachedActivity !== activity) return
        stopListening()
        container?.let { c ->
            (c.parent as? ViewGroup)?.removeView(c)
        }
        container = null
        attachedActivity = null
        pickLauncher = null
        configureLauncher = null
    }

    fun startListening() {
        val h = host ?: return
        if (_listening.value) return
        runCatching { h.startListening() }
        _listening.value = true
    }

    fun stopListening() {
        val h = host ?: return
        if (!_listening.value) return
        runCatching { h.stopListening() }
        _listening.value = false
    }

    /** Open system widget picker; place at [yawDeg]/[pitchDeg] on the sphere when done. */
    fun openPicker(yawDeg: Float, pitchDeg: Float) {
        val h = host
        val launcher = pickLauncher
        if (h == null || launcher == null) {
            Log.w(LOG_TAG, "openPicker: host not bound")
            return
        }
        pendingYawDeg = yawDeg
        pendingPitchDeg = pitchDeg
        val id = h.allocateAppWidgetId()
        pendingWidgetId = id
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            id,
        )
        launcher.launch(intent)
    }

    fun onPickResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            deletePendingId()
            return
        }
        val id = data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (id == -1) {
            deletePendingId()
            return
        }
        pendingWidgetId = id
        configureOrAdd(id)
    }

    fun onConfigureResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            deletePendingId()
            return
        }
        val id = data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
            ?: pendingWidgetId
        if (id == -1) {
            deletePendingId()
            return
        }
        addWidgetToDesk(id)
    }

    fun hostView(appWidgetId: Int): AppWidgetHostView? = hostViews[appWidgetId]

    fun captureBitmap(appWidgetId: Int, force: Boolean = false): Bitmap? {
        val view = hostViews[appWidgetId] ?: return null
        val info = manager?.getAppWidgetInfo(appWidgetId) ?: return null
        val placed = HomeSpaceDeskState.placed.firstOrNull {
            it.app.kind == HomeSpaceDesk.Kind.WIDGET &&
                DeskWidgetUtils.parseWidgetId(it.app.componentKey) == appWidgetId
        } ?: return null
        val halfW = placed.halfWidth ?: HomeSpaceDesk.ICON_HALF_WIDTH * 3f
        val halfH = placed.halfHeight ?: halfW
        val ctx = appContext ?: return null
        if (!WidgetCaptureCoordinator.shouldCapture(appWidgetId, force || appWidgetId in dirtyIds)) {
            return null
        }
        WidgetCaptureCoordinator.markCaptureStarted(appWidgetId)
        return try {
            DeskWidgetUtils.configureHostView(view, ctx, info, appWidgetId, halfW, halfH)
            DeskWidgetUtils.captureBitmap(view)?.also {
                dirtyIds.remove(appWidgetId)
            }
        } finally {
            WidgetCaptureCoordinator.markCaptureFinished(appWidgetId)
        }
    }

    fun deleteWidget(appWidgetId: Int) {
        hostViews.remove(appWidgetId)?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        WidgetCaptureCoordinator.clear(appWidgetId)
        dirtyIds.remove(appWidgetId)
        runCatching { host?.deleteAppWidgetId(appWidgetId) }
    }

    fun markDirty(appWidgetId: Int) {
        dirtyIds += appWidgetId
    }

    private fun configureOrAdd(id: Int) {
        val info = manager?.getAppWidgetInfo(id)
        val configure = info?.configure
        if (configure != null) {
            val launcher = configureLauncher
            if (launcher == null) {
                addWidgetToDesk(id)
                return
            }
            try {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .setComponent(configure)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                launcher.launch(intent)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "configure unavailable ${configure.className}: ${e.message}")
                addWidgetToDesk(id)
            }
        } else {
            addWidgetToDesk(id)
        }
    }

    private fun addWidgetToDesk(id: Int) {
        val ctx = appContext ?: return
        val info = manager?.getAppWidgetInfo(id) ?: run {
            Log.w(LOG_TAG, "addWidget: no provider id=$id")
            runCatching { host?.deleteAppWidgetId(id) }
            pendingWidgetId = -1
            return
        }
        val h = host ?: return
        try {
            ensureContainer(attachedActivity)
            val view = h.createView(
                ContextThemeWrapper(ctx, android.R.style.Theme_DeviceDefault),
                id,
                info,
            )
            container?.addView(view)
            hostViews[id] = view
            val (halfW, halfH) = DeskWidgetUtils.defaultHalfExtents(info)
            val label = info.loadLabel(ctx.packageManager)?.toString()
                ?: info.provider.className.substringAfterLast('.')
            HomeSpaceDeskState.placeWidget(
                appWidgetId = id,
                label = label,
                packageName = info.provider.packageName,
                yawDeg = pendingYawDeg,
                pitchDeg = pendingPitchDeg,
                halfWidth = halfW,
                halfHeight = halfH,
            )
            dirtyIds += id
            _changed.tryEmit(id)
            Log.d(LOG_TAG, "added widget id=$id provider=${info.provider} at yaw=$pendingYawDeg")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "addWidget failed id=$id", e)
            runCatching { h.deleteAppWidgetId(id) }
        }
        pendingWidgetId = -1
    }

    private fun restoreHostViewsForPlaced() {
        val ctx = appContext ?: return
        val h = host ?: return
        val mgr = manager ?: return
        HomeSpaceDeskState.placed
            .filter { it.app.kind == HomeSpaceDesk.Kind.WIDGET }
            .forEach { placed ->
                val id = DeskWidgetUtils.parseWidgetId(placed.app.componentKey) ?: return@forEach
                if (hostViews.containsKey(id)) return@forEach
                val info = mgr.getAppWidgetInfo(id) ?: return@forEach
                runCatching {
                    val view = h.createView(
                        ContextThemeWrapper(ctx, android.R.style.Theme_DeviceDefault),
                        id,
                        info,
                    )
                    container?.addView(view)
                    hostViews[id] = view
                    dirtyIds += id
                }.onFailure {
                    Log.w(LOG_TAG, "restore host view failed id=$id: ${it.message}")
                }
            }
    }

    private fun ensureContainer(activity: Activity?) {
        if (activity == null) return
        if (container != null && attachedActivity === activity) return
        container?.let { c -> (c.parent as? ViewGroup)?.removeView(c) }
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val frame = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            // Off-screen so host views stay attached without covering UI.
            translationX = -10_000f
        }
        root.addView(frame)
        container = frame
        attachedActivity = activity
        // Re-parent existing views into the new container.
        hostViews.values.forEach { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            frame.addView(view)
        }
    }

    private fun deletePendingId() {
        val id = pendingWidgetId
        pendingWidgetId = -1
        if (id != -1) runCatching { host?.deleteAppWidgetId(id) }
    }
}

/** Contracts for [DeskWidgetController] pick / configure flows. */
object DeskWidgetContracts {
    val Pick = ActivityResultContracts.StartActivityForResult()
    val Configure = ActivityResultContracts.StartActivityForResult()
}
