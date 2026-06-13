package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGeometry
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceCylinderGrid
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceGlesConfig
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceWraparound
import dev.electrikjesus.xrlauncher.ui.spatial.WorkspaceGlesBackdrop

/** Shared workspace viewport (px) for consistent cylinder arc math across panels. */
val LocalWorkspaceViewportPx = compositionLocalOf { IntSize.Zero }

@Composable
fun WorkspaceWraparoundLayer(
    appearance: WorkspaceAppearance,
    cursorX: Float,
    cursorY: Float,
    panels: List<PanelState> = emptyList(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tuned = appearance.clamped()
    val spanX = tuned.workspaceWidth
    val spanY = tuned.workspaceHeight
    val lookYaw = WorkspaceWraparound.effectiveLookYaw(tuned)
    val lookPitch = WorkspaceWraparound.effectiveLookPitch(tuned)
    val camera = WorkspaceCylinderGeometry.cameraState(
        cursorX = cursorX,
        cursorY = cursorY,
        lookYawDegrees = lookYaw,
        lookPitchDegrees = lookPitch,
        workspaceWidth = spanX,
        workspaceHeight = spanY,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val panPxX = camera.panNormX * viewportWidthPx
        val panPxY = camera.panNormY * viewportHeightPx
        val focalPx = viewportWidthPx * WorkspaceCylinderGeometry.FOCAL_LENGTH_VIEWPORT_FRACTION
        val glesPresentation = WorkspaceGlesConfig.texturedPanelsEnabled && tuned.wrapCurvature > 0.01f
        val panelGuideCenters = WorkspaceCylinderGrid.guideSlotsForPanels(panels)
        val glesEnabled = tuned.wrapCurvature > 0.01f && (
            WorkspaceGlesConfig.showWallpaperCylinder ||
                WorkspaceGlesConfig.texturedPanelsEnabled ||
                WorkspaceGlesConfig.showGuideWireframe
            )

        if (glesEnabled) {
            WorkspaceGlesBackdrop(
                camera = camera,
                curvature = tuned.wrapCurvature,
                workspaceWidth = spanX,
                workspaceHeight = spanY,
                wallpaperChoice = tuned.wallpaperChoice,
                panelGuideCenters = panelGuideCenters,
                showWallpaperCylinder = WorkspaceGlesConfig.showWallpaperCylinder,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val viewportProvider = @Composable {
            CompositionLocalProvider(
                LocalWorkspaceViewportPx provides IntSize(
                    viewportWidthPx.toInt(),
                    viewportHeightPx.toInt(),
                ),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }

        if (glesPresentation) {
            // Flat layout for capture + hit targets; camera lives in GLES only.
            viewportProvider()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = camera.yawDegrees
                        rotationX = camera.pitchDegrees
                        translationX = panPxX
                        translationY = panPxY
                        scaleX = spanX
                        scaleY = spanY
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                        cameraDistance = focalPx
                    },
                contentAlignment = Alignment.Center,
            ) {
                viewportProvider()
            }
        }
    }
}

@Composable
fun WraparoundPanelContainer(
    centerXNorm: Float,
    centerYNorm: Float,
    wrapCurvature: Float,
    workspaceWidth: Float = 1f,
    workspaceHeight: Float = 1f,
    panelId: String? = null,
    /** When false, only rotate/foreshorten — freeform panels keep their saved bounds. */
    applyArcPositionShift: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val widthCompensation = if (workspaceWidth > 1f) 1f / workspaceWidth else 1f
    val heightCompensation = if (workspaceHeight > 1f) 1f / workspaceHeight else 1f
    val workspaceViewport = LocalWorkspaceViewportPx.current
    val viewportW = workspaceViewport.width.toFloat()
    val viewportH = workspaceViewport.height.toFloat()
    val glesPresentation = WorkspaceGlesConfig.texturedPanelsEnabled &&
        wrapCurvature > 0.01f &&
        panelId != null
    val hideComposePanel = glesPresentation && WorkspaceGlesConfig.hideComposePanelsWhenGles

    val placement = if (viewportW > 0f && viewportH > 0f) {
        WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = centerXNorm,
            centerYNorm = centerYNorm,
            curvature = wrapCurvature,
            workspaceWidth = workspaceWidth,
            workspaceHeight = workspaceHeight,
            viewportWidthPx = viewportW,
            viewportHeightPx = viewportH,
        )
    } else {
        WorkspaceCylinderGeometry.PanelPlacement(0f, 0f, 0f, 0f, 1f)
    }

    val focalPx = viewportW * WorkspaceCylinderGeometry.FOCAL_LENGTH_VIEWPORT_FRACTION

    val panelContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (glesPresentation) {
                        Modifier
                    } else {
                        Modifier.graphicsLayer {
                            translationX = if (applyArcPositionShift) placement.arcShiftXPx else 0f
                            translationY = if (applyArcPositionShift) placement.arcShiftYPx else 0f
                            rotationY = placement.rotationYDeg
                            rotationX = placement.rotationXDeg
                            scaleX = placement.scale * widthCompensation
                            scaleY = placement.scale * heightCompensation
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                            cameraDistance = focalPx.coerceAtLeast(1f)
                        }
                    },
                ),
        ) {
            content()
        }
    }

    Box(modifier = modifier) {
        if (glesPresentation) {
            PanelTextureCapture(
                panelId = panelId!!,
                centerXNorm = centerXNorm,
                centerYNorm = centerYNorm,
                enabled = true,
                drawToScreen = !hideComposePanel,
                modifier = Modifier.fillMaxSize(),
            ) {
                panelContent()
            }
        } else {
            panelContent()
        }
    }
}
