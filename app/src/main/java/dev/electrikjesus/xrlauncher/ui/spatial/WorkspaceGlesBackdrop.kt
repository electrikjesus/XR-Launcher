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
import dev.electrikjesus.xrlauncher.core.launcher.WorkspaceWallpaperResolver
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeSpace3d
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpacePaneSlot
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWallpaperChoice
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGrid
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
    wallpaperChoice: WorkspaceWallpaperChoice = WorkspaceWallpaperChoice.SYSTEM,
    panelGuideCenters: List<WorkspaceCylinderGrid.SlotCenter> = emptyList(),
    showWallpaperCylinder: Boolean = true,
    surroundRoom: Boolean = false,
    roomRadius: Float = GlassesHomeSpace3d.ROOM_RADIUS,
    homeSpaceSlots: List<HomeSpacePaneSlot> = emptyList(),
    homeSpacePanelScale: Float = 1f,
    homeSpaceSphereScale: Float = 1f,
    homeSpacePanesEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = surroundRoom || (curvature > 0.01f && (
        WorkspaceGlesConfig.showWallpaperCylinder ||
            WorkspaceGlesConfig.texturedPanelsEnabled ||
            WorkspaceGlesConfig.showGuideWireframe
        )),
) {
    if (!enabled) return

    val context = LocalContext.current.applicationContext
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var wallpaperGeneration by remember { mutableIntStateOf(0) }

    DisposableEffect(context, wallpaperChoice) {
        if (wallpaperChoice != WorkspaceWallpaperChoice.SYSTEM) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }
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

    androidx.compose.runtime.LaunchedEffect(context, wallpaperGeneration, wallpaperChoice) {
        wallpaperBitmap = withContext(Dispatchers.IO) {
            WorkspaceWallpaperResolver.resolveBitmap(context, wallpaperChoice)
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

    DisposableEffect(curvature, workspaceWidth, workspaceHeight, surroundRoom, roomRadius, homeSpaceSlots, homeSpacePanelScale, homeSpaceSphereScale, homeSpacePanesEnabled) {
        renderer.curvature = curvature
        renderer.workspaceWidth = workspaceWidth
        renderer.workspaceHeight = workspaceHeight
        renderer.surroundRoom = surroundRoom
        renderer.roomRadius = roomRadius
        renderer.homeSpaceSlots = homeSpaceSlots
        renderer.homeSpacePanelScale = homeSpacePanelScale
        renderer.homeSpaceSphereScale = homeSpaceSphereScale
        renderer.homeSpacePanesEnabled = homeSpacePanesEnabled
        renderer.rebuildCylinderMesh()
        onDispose { }
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
                setZOrderOnTop(false)
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
            renderer.panelGuideCenters = panelGuideCenters
            renderer.showWallpaperCylinder = showWallpaperCylinder
            renderer.surroundRoom = surroundRoom
            renderer.roomRadius = roomRadius
            renderer.homeSpaceSlots = homeSpaceSlots
            renderer.homeSpacePanelScale = homeSpacePanelScale
            renderer.homeSpaceSphereScale = homeSpaceSphereScale
            renderer.homeSpacePanesEnabled = homeSpacePanesEnabled
            renderer.setPanelTextures(WorkspacePanelTextureBus.snapshot())
            view.requestRender()
        },
    )
}
