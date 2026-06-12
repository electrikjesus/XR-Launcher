package dev.electrikjesus.xrlauncher.ui.spatial

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.electrikjesus.xrlauncher.core.launcher.SystemWallpaperLoader
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceGlesConfig
import dev.electrikjesus.xrlauncher.core.workspace.WorkspacePanelTextureBus
import dev.electrikjesus.xrlauncher.ui.spatial.gles.CylinderGlRenderer
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GLES inner-cylinder scene — wallpaper backdrop, wireframe guides, textured panel quads.
 */
@Composable
fun WorkspaceGlesBackdrop(
    camera: WorkspaceCylinderGeometry.CameraState,
    curvature: Float,
    workspaceWidth: Float,
    workspaceHeight: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = curvature > 0.01f && (
        WorkspaceGlesConfig.showWallpaperCylinder ||
            WorkspaceGlesConfig.texturedPanelsEnabled ||
            WorkspaceGlesConfig.showGuideWireframe
        ),
) {
    if (!enabled) return

    val context = LocalContext.current.applicationContext
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var wallpaperGeneration by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
                    wallpaperGeneration++
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    androidx.compose.runtime.LaunchedEffect(context, wallpaperGeneration) {
        wallpaperBitmap = withContext(Dispatchers.IO) {
            SystemWallpaperLoader.loadBitmap(context)
        }
    }

    val renderer = remember { CylinderGlRenderer() }
    val surfaceViewRef = remember { AtomicReference<GLSurfaceView?>(null) }
    val renderCallback = remember {
        {
            surfaceViewRef.get()?.requestRender()
            Unit
        }
    }

    DisposableEffect(curvature, workspaceWidth, workspaceHeight) {
        renderer.curvature = curvature
        renderer.workspaceWidth = workspaceWidth
        renderer.workspaceHeight = workspaceHeight
        renderer.rebuildCylinderMesh()
        onDispose {
            WorkspacePanelTextureBus.unregisterRenderCallback(renderCallback)
            WorkspacePanelTextureBus.clear()
        }
    }

    DisposableEffect(Unit) {
        WorkspacePanelTextureBus.registerRenderCallback(renderCallback)
        onDispose { WorkspacePanelTextureBus.unregisterRenderCallback(renderCallback) }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            GLSurfaceView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                surfaceViewRef.set(this)
            }
        },
        update = { view ->
            surfaceViewRef.set(view)
            renderer.camera = camera
            renderer.curvature = curvature
            renderer.workspaceWidth = workspaceWidth
            renderer.workspaceHeight = workspaceHeight
            renderer.setWallpaperBitmap(wallpaperBitmap, wallpaperGeneration.toLong())
            renderer.setPanelTextures(WorkspacePanelTextureBus.snapshot())
            view.requestRender()
        },
    )
}
