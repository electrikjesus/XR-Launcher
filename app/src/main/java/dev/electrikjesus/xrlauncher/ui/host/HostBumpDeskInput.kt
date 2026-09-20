package dev.electrikjesus.xrlauncher.ui.host

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.HostInputMethod
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialog
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.LauncherContextMenuState

/**
 * BumpDesk-derived host pointer for Expanded Home Space.
 *
 * Touch / button events: [pointerInteropFilter] (GLES AndroidView ate parent pointerInput).
 * Mouse hover (no button): Compose [pointerInput] Move — classic FPS look without click.
 * Top HUD is a wrap-content sibling above this catcher in [HostHomeSpaceScreen].
 * Settings / Edit modals own the pointer — the catcher is removed while they are open.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HostBumpDeskInput(
    modifier: Modifier = Modifier,
    onZoomSphere: (delta: Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val zoomLatest by rememberUpdatedState(onZoomSphere)
    var catcherW by remember { mutableIntStateOf(1) }
    var catcherH by remember { mutableIntStateOf(1) }
    val editing by GlassesSessionState.homeSpaceEditFlow.collectAsState()
    val hostDialog by HomeSpaceDialogState.dialogFlow.collectAsState()
    val contextMenu by LauncherContextMenuState.request.collectAsState()
    // Settings / Edit / radial menu own the pointer — catcher would eat their clicks.
    val modalOpen = editing || hostDialog != HomeSpaceDialog.NONE || contextMenu != null

    LaunchedEffect(zoomLatest) {
        HostBumpDeskMotionBridge.bind(zoomLatest)
        Log.d(
            "HostBumpDesk",
            "composed+bound host=${GlassesSessionState.hostImmersiveSession} " +
                "method=${HostInputMethod.preference} pref=${GlassesLookMode.preference}",
        )
    }
    DisposableEffect(Unit) {
        onDispose { HostBumpDeskMotionBridge.reset() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        // While Settings/Edit are open, Compose owns clicks (back, scroll, +/-). Leaving the
        // catcher up would consume MotionEvents and never emit LeftClick for modals.
        if (!modalOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent) // empty catchers are not hit-tested without a draw
                    .onSizeChanged {
                        catcherW = it.width.coerceAtLeast(1)
                        catcherH = it.height.coerceAtLeast(1)
                    }
                    // Hover mouse-look (no buttons). Pressed paths stay on interop to avoid double-fire.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                if (event.type != PointerEventType.Move &&
                                    event.type != PointerEventType.Enter
                                ) {
                                    continue
                                }
                                if (event.changes.any { it.pressed }) continue
                                val change = event.changes.firstOrNull() ?: continue
                                val hover = MotionEvent.obtain(
                                    /* downTime */ 0L,
                                    /* eventTime */ System.currentTimeMillis(),
                                    MotionEvent.ACTION_HOVER_MOVE,
                                    change.position.x,
                                    change.position.y,
                                    0,
                                )
                                try {
                                    HostBumpDeskMotionBridge.onTouch(hover, catcherW, catcherH)
                                } finally {
                                    hover.recycle()
                                }
                            }
                        }
                    }
                    .pointerInteropFilter { event ->
                        HostBumpDeskMotionBridge.onTouch(event, catcherW, catcherH)
                    },
            )
        }
    }
}
