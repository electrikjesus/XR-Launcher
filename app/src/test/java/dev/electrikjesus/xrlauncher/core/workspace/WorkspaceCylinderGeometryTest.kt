package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WorkspaceCylinderGeometryTest {
    @Test
    fun panelPlacement_leftPanelRotatesInward() {
        val left = WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = 0f,
            centerYNorm = 0.5f,
            curvature = 1f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        val right = WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = 1f,
            centerYNorm = 0.5f,
            curvature = 1f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(left.rotationYDeg > 0f)
        assertTrue(right.rotationYDeg < 0f)
    }

    @Test
    fun panelPlacement_centerIsIdentity() {
        val center = WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = 0.5f,
            centerYNorm = 0.5f,
            curvature = 1f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertEquals(0f, center.rotationYDeg, 0.01f)
        assertEquals(0f, center.rotationXDeg, 0.01f)
        assertEquals(0f, center.arcShiftXPx, 1f)
    }

    @Test
    fun panelPlacement_edgeForeshortens() {
        val edge = WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = 0f,
            centerYNorm = 0.5f,
            curvature = 1f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        val center = WorkspaceCylinderGeometry.panelPlacement(
            centerXNorm = 0.5f,
            centerYNorm = 0.5f,
            curvature = 1f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(edge.scale < center.scale)
    }

    @Test
    fun cameraState_invertsLookForSceneRotation() {
        val camera = WorkspaceCylinderGeometry.cameraState(
            cursorX = 0.5f,
            cursorY = 0.5f,
            lookYawDegrees = 20f,
            lookPitchDegrees = -5f,
            workspaceWidth = 1f,
            workspaceHeight = 0.7f,
        )
        assertEquals(-20f, camera.yawDegrees, 0.001f)
        assertEquals(5f, camera.pitchDegrees, 0.001f)
    }
}
