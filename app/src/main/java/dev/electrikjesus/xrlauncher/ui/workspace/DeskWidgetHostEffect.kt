package dev.electrikjesus.xrlauncher.ui.workspace

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconSnapshot
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconTextureBus
import dev.electrikjesus.xrlauncher.core.workspace.DeskWidgetController
import dev.electrikjesus.xrlauncher.core.workspace.DeskWidgetContracts
import dev.electrikjesus.xrlauncher.core.workspace.DeskWidgetUtils
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDeskState
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Binds [DeskWidgetController] to the host Activity (pick / configure / off-screen views)
 * and refreshes widget bitmaps into [DeskIconTextureBus].
 */
@Composable
fun DeskWidgetHostEffect() {
    val context = LocalContext.current
    val activity = context.findActivity()
    val pickLauncher = rememberLauncherForActivityResult(DeskWidgetContracts.Pick) { result ->
        DeskWidgetController.onPickResult(result.resultCode, result.data)
    }
    val configureLauncher = rememberLauncherForActivityResult(DeskWidgetContracts.Configure) { result ->
        DeskWidgetController.onConfigureResult(result.resultCode, result.data)
    }
    val placed by HomeSpaceDeskState.placedFlow.collectAsState()
    val placedLatest = rememberUpdatedState(placed)

    DisposableEffect(activity, pickLauncher, configureLauncher) {
        if (activity != null) {
            DeskWidgetController.bindActivity(activity, pickLauncher, configureLauncher)
        }
        onDispose {
            if (activity != null) DeskWidgetController.unbindActivity(activity)
        }
    }

    LaunchedEffect(placed) {
        if (activity != null && placed.any { it.app.kind == HomeSpaceDesk.Kind.WIDGET }) {
            DeskWidgetController.bindActivity(activity, pickLauncher, configureLauncher)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            refreshWidgetSnapshots(placedLatest.value)
            delay(1_500)
        }
    }

    LaunchedEffect(Unit) {
        DeskWidgetController.changed.collect { id ->
            DeskWidgetController.markDirty(id)
            refreshWidgetSnapshots(placedLatest.value, forceId = id)
        }
    }
}

private fun refreshWidgetSnapshots(
    placed: List<HomeSpaceDesk.Placed>,
    forceId: Int? = null,
) {
    val widgets = placed.filter { it.app.kind == HomeSpaceDesk.Kind.WIDGET }
    if (widgets.isEmpty()) return
    val existing = DeskIconTextureBus.snapshots().associateBy { it.componentKey }.toMutableMap()
    var changed = false
    widgets.forEach { item ->
        val id = DeskWidgetUtils.parseWidgetId(item.app.componentKey) ?: return@forEach
        val force = forceId == id || existing[item.app.componentKey] == null
        val bitmap = DeskWidgetController.captureBitmap(id, force = force) ?: return@forEach
        existing[item.app.componentKey] = DeskIconSnapshot(
            componentKey = item.app.componentKey,
            bitmap = bitmap,
            generation = System.nanoTime(),
        )
        changed = true
    }
    if (!changed) return
    val icons = DeskIconTextureBus.icons()
    if (icons.isEmpty()) return
    // Keep 1:1 icon/snapshot length for the GLES atlas upload.
    val snapshots = icons.map { icon -> existing[icon.componentKey] ?: return }
    DeskIconTextureBus.set(icons, snapshots)
}

private fun android.content.Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
