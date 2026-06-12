package dev.electrikjesus.xrlauncher.ui.spatial

import android.opengl.GLSurfaceView
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.ui.spatial.gles.CylinderGlRenderer

/**
 * GLES inner-cylinder guide mesh behind Compose panels — same camera as [WorkspaceWraparoundLayer].
 */
@Composable
fun WorkspaceGlesBackdrop(
    camera: WorkspaceCylinderGeometry.CameraState,
    curvature: Float,
    workspaceWidth: Float,
    workspaceHeight: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = curvature > 0.01f,
) {
    if (!enabled) return

    val renderer = remember { CylinderGlRenderer() }

    DisposableEffect(curvature, workspaceWidth, workspaceHeight) {
        renderer.curvature = curvature
        renderer.workspaceWidth = workspaceWidth
        renderer.workspaceHeight = workspaceHeight
        renderer.rebuildCylinderMesh()
        onDispose { }
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
            }
        },
        update = { view ->
            renderer.camera = camera
            renderer.curvature = curvature
            renderer.workspaceWidth = workspaceWidth
            renderer.workspaceHeight = workspaceHeight
            view.requestRender()
        },
    )
}
