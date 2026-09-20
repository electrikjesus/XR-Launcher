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
import dev.electrikjesus.xrlauncher.core.workspace.DeskIconTextureBus
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeSpace3d
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpacePaneSlot
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWallpaperChoice
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGrid
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceGlesConfig
import dev.electrikjesus.xrlauncher.core.workspace.WorkspacePanelTextureBus
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.ui.host.HostBumpDeskMotionBridge
import dev.electrikjesus.xrlauncher.ui.spatial.gles.CylinderGlRenderer
import java.util.concurrent.atomic.AtomicReference

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
    hdriAssetId: String = "",
    panelGuideCenters: List<WorkspaceCylinderGrid.SlotCenter> = emptyList(),
    showWallpaperCylinder: Boolean = true,
    surroundRoom: Boolean = false,
    roomRadius: Float = GlassesHomeSpace3d.ROOM_RADIUS,
    homeSpaceSlots: List<HomeSpacePaneSlot> = emptyList(),
    homeSpacePanelScale: Float = 1f,
    homeSpaceSphereScale: Float = 1f,
    homeSpacePanesEnabled: Boolean = false,
    cursorX: Float = 0.5f,
    cursorY: Float = 0.5f,
    showSphereCursor: Boolean = false,
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
    var wallpaperUploadEpoch by remember { mutableIntStateOf(0) }
    val equirectangular = wallpaperChoice == WorkspaceWallpaperChoice.POLY_HAVEN

    // Drop the previous preset immediately so we never upload a stale gradient under the
    // new choice's generation key (that used to skip the real SYSTEM bitmap upload).
    androidx.compose.runtime.LaunchedEffect(wallpaperChoice, hdriAssetId) {
        wallpaperBitmap = null
        wallpaperGeneration++
    }

    // Re-load after returning from All-files settings (SYSTEM needs MANAGE_EXTERNAL_STORAGE).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, wallpaperChoice) {
        if (wallpaperChoice != WorkspaceWallpaperChoice.SYSTEM) {
            return@DisposableEffect onDispose { }
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                wallpaperGeneration++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context, wallpaperChoice) {
        if (wallpaperChoice != WorkspaceWallpaperChoice.SYSTEM) {
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

    androidx.compose.runtime.LaunchedEffect(context, wallpaperGeneration, wallpaperChoice, hdriAssetId) {
        val choice = wallpaperChoice
        val assetId = hdriAssetId
        val bitmap = WorkspaceWallpaperResolver.resolveBitmap(context, choice, assetId)
        if (choice != wallpaperChoice || assetId != hdriAssetId) return@LaunchedEffect
        wallpaperBitmap = bitmap
        wallpaperUploadEpoch++
    }

    val renderer = remember { CylinderGlRenderer() }
    val surfaceViewRef = remember { AtomicReference<GLSurfaceView?>(null) }
    val renderCallback = remember {
        {
            val view = surfaceViewRef.get()
            if (view != null) {
                // Bus updates can land without a Compose recompose (e.g. All Apps open).
                // Sync renderer state here or WHEN_DIRTY redraws the previous desk frame.
                renderer.deskIcons = DeskIconTextureBus.icons()
                renderer.deskHoveredKey = DeskIconTextureBus.hoveredKey()
                renderer.setDeskTextures(DeskIconTextureBus.snapshots())
                renderer.setPanelTextures(WorkspacePanelTextureBus.snapshot())
                view.requestRender()
            }
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
        DeskIconTextureBus.registerRenderCallback(renderCallback)
        onDispose {
            WorkspacePanelTextureBus.unregisterRenderCallback(renderCallback)
            DeskIconTextureBus.unregisterRenderCallback(renderCallback)
        }
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
                // Host BumpDesk look/desk: SurfaceView MotionEvents never reach Compose
                // pointerInput. Forward them while leaving top-HUD chrome to Compose.
                setOnTouchListener { v, event ->
                    if (!GlassesSessionState.hostImmersiveSession) return@setOnTouchListener false
                    HostBumpDeskMotionBridge.onTouch(event, v.width, v.height)
                }
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                surfaceViewRef.set(this)
            }
        },
        update = { view ->
            surfaceViewRef.set(view)
            view.setOnTouchListener { v, event ->
                if (!GlassesSessionState.hostImmersiveSession) return@setOnTouchListener false
                HostBumpDeskMotionBridge.onTouch(event, v.width, v.height)
            }
            renderer.camera = camera
            renderer.curvature = curvature
            renderer.workspaceWidth = workspaceWidth
            renderer.workspaceHeight = workspaceHeight
            // Monotonic uploadEpoch so a finished load always re-uploads even when the
            // choice ordinal + wallpaperGeneration pair collided with a stale frame.
            wallpaperBitmap?.let { bitmap ->
                renderer.setWallpaperBitmap(
                    bitmap,
                    wallpaperChoice.ordinal * 1_000_000L + wallpaperUploadEpoch,
                )
            }
            renderer.panelGuideCenters = panelGuideCenters
            renderer.showWallpaperCylinder = showWallpaperCylinder
            renderer.surroundRoom = surroundRoom
            renderer.roomRadius = roomRadius
            if (renderer.wallpaperEquirectangular != equirectangular) {
                renderer.wallpaperEquirectangular = equirectangular
                renderer.rebuildCylinderMesh()
            } else {
                renderer.wallpaperEquirectangular = equirectangular
            }
            renderer.homeSpaceSlots = homeSpaceSlots
            renderer.homeSpacePanelScale = homeSpacePanelScale
            renderer.homeSpaceSphereScale = homeSpaceSphereScale
            renderer.homeSpacePanesEnabled = homeSpacePanesEnabled
            renderer.cursorX = cursorX
            renderer.cursorY = cursorY
            renderer.showSphereCursor = showSphereCursor
            renderer.deskIcons = DeskIconTextureBus.icons()
            renderer.deskHoveredKey = DeskIconTextureBus.hoveredKey()
            renderer.setDeskTextures(DeskIconTextureBus.snapshots())
            renderer.setPanelTextures(WorkspacePanelTextureBus.snapshot())
            view.requestRender()
        },
    )
}
