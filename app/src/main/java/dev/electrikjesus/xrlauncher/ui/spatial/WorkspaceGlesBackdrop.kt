package dev.electrikjesus.xrlauncher.ui.spatial

import android.opengl.GLSurfaceView
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceGlesConfig
import dev.electrikjesus.xrlauncher.core.workspace.WorkspacePanelTextureBus
import dev.electrikjesus.xrlauncher.ui.spatial.gles.CylinderGlRenderer
import java.util.concurrent.atomic.AtomicReference

/**
 * GLES inner-cylinder scene behind Compose — wireframe guides and textured panel quads.
 */
@Composable
fun WorkspaceGlesBackdrop(
    camera: WorkspaceCylinderGeometry.CameraState,
    curvature: Float,
    workspaceWidth: Float,
    workspaceHeight: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = curvature > 0.01f &&
        (WorkspaceGlesConfig.texturedPanelsEnabled || WorkspaceGlesConfig.showGuideWireframe),
) {
    if (!enabled) return

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
        factory = { context ->
            GLSurfaceView(context).apply {
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
            renderer.setPanelTextures(WorkspacePanelTextureBus.snapshot())
            view.requestRender()
        },
    )
}
