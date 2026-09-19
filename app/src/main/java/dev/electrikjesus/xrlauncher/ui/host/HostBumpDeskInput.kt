package dev.electrikjesus.xrlauncher.ui.host

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.input.HostInputMethod
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode

/**
 * BumpDesk-derived host pointer for Expanded Home Space.
 *
 * Full-screen [pointerInteropFilter] catcher above GLES — Compose pointerInput never saw
 * desk swipes over AndroidView (interop hits the embedded SurfaceView instead). Top HUD is
 * a wrap-content sibling above this catcher in [HostHomeSpaceScreen].
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent) // empty catchers are not hit-tested without a draw
                .onSizeChanged {
                    catcherW = it.width.coerceAtLeast(1)
                    catcherH = it.height.coerceAtLeast(1)
                }
                .pointerInteropFilter { event ->
                    HostBumpDeskMotionBridge.onTouch(event, catcherW, catcherH)
                },
        )
    }
}
