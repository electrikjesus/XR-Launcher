package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.GlassesXrInputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WorkspaceWraparoundTest {
    @Test
    fun panelTransform_flatWhenCurvatureZero() {
        val transform = WorkspaceWraparound.panelTransform(0.25f, 0.5f, curvature = 0f)
        assertEquals(0f, transform.rotationY, 0.001f)
        assertEquals(0f, transform.rotationX, 0.001f)
        assertEquals(1f, transform.scale, 0.001f)
    }

    @Test
    fun panelTransform_leftAndRightFaceViewerAtCenter() {
        val left = WorkspaceWraparound.panelTransform(0f, 0.5f, curvature = 1f)
        val center = WorkspaceWraparound.panelTransform(0.5f, 0.5f, curvature = 1f)
        val right = WorkspaceWraparound.panelTransform(1f, 0.5f, curvature = 1f)
        assertTrue(left.rotationY > 0f)
        assertEquals(0f, center.rotationY, 0.001f)
        assertTrue(right.rotationY < 0f)
    }

    @Test
    fun panelTransform_topAndBottomFaceViewerAtCenter() {
        val top = WorkspaceWraparound.panelTransform(0.5f, 0f, curvature = 1f)
        val bottom = WorkspaceWraparound.panelTransform(0.5f, 1f, curvature = 1f)
        assertTrue(top.rotationX > 0f)
        assertTrue(bottom.rotationX < 0f)
    }

    @Test
    fun panelTransform_edgeForeshortensOnArc() {
        val edge = WorkspaceWraparound.panelTransform(0f, 0.5f, curvature = 1f)
        val center = WorkspaceWraparound.panelTransform(0.5f, 0.5f, curvature = 1f)
        assertTrue(edge.scale < center.scale)
    }

    @Test
    fun panelTransform_widerWorkspaceIncreasesEdgeYaw() {
        val narrow = WorkspaceWraparound.panelTransform(0f, 0.5f, curvature = 1f, workspaceWidth = 0.8f)
        val wide = WorkspaceWraparound.panelTransform(0f, 0.5f, curvature = 1f, workspaceWidth = 1.4f)
        assertTrue(abs(wide.rotationY) > abs(narrow.rotationY))
    }

    @Test
    fun workspaceLayerPan_cursorAtEdgeMovesAtSpanOne() {
        val edge = WorkspaceWraparound.workspaceLayerPan(
            cursorX = 1f,
            cursorY = 0.5f,
            lookYawDegrees = 0f,
            lookPitchDegrees = 0f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
        )
        assertTrue(edge.first > 0f)
        assertEquals(0f, edge.second, 0.001f)
    }

    @Test
    fun workspaceLayerPan_wideSpanPansMoreThanSpanOne() {
        val atOne = WorkspaceWraparound.workspaceLayerPan(
            cursorX = 1f,
            cursorY = 0.5f,
            lookYawDegrees = 0f,
            lookPitchDegrees = 0f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
        )
        val atWide = WorkspaceWraparound.workspaceLayerPan(
            cursorX = 1f,
            cursorY = 0.5f,
            lookYawDegrees = 0f,
            lookPitchDegrees = 0f,
            workspaceWidth = 1.4f,
            workspaceHeight = 1f,
        )
        assertTrue(atWide.first > atOne.first)
    }

    @Test
    fun backdropLook_includesCursorParallax() {
        val center = WorkspaceWraparound.backdropLook(0.5f, 0.5f, 0f, 0f)
        val edge = WorkspaceWraparound.backdropLook(1f, 0.5f, 0f, 0f)
        assertEquals(0f, center.first, 0.001f)
        assertTrue(edge.first > 0f)
    }

    @Test
    fun arcTranslationPx_leftPanelShiftsTowardCenterOnSharedViewport() {
        val viewportW = 1920f
        val (shiftX, _) = WorkspaceWraparound.arcTranslationPx(
            centerX = 0.17f,
            centerY = 0.5f,
            curvature = 0.35f,
            workspaceWidth = 1f,
            workspaceHeight = 0.7f,
            viewportWidthPx = viewportW,
            viewportHeightPx = 1080f,
        )
        assertTrue(shiftX > 0f)
    }

    @Test
    fun arcTranslationPx_centerPanelHasNoShift() {
        val (shiftX, shiftY) = WorkspaceWraparound.arcTranslationPx(
            centerX = 0.5f,
            centerY = 0.5f,
            curvature = 1f,
            workspaceWidth = 1f,
            workspaceHeight = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertEquals(0f, shiftX, 1f)
        assertEquals(0f, shiftY, 1f)
    }

    @Test
    fun effectiveLook_sumsAppearanceAndLiveOffset() {
        WorkspaceLookOffset.setOffset(5f, -3f)
        try {
            val appearance = WorkspaceAppearance(lookYawDegrees = 10f, lookPitchDegrees = 2f)
            assertEquals(15f, WorkspaceWraparound.effectiveLookYaw(appearance), 0.001f)
            assertEquals(-1f, WorkspaceWraparound.effectiveLookPitch(appearance), 0.001f)
        } finally {
            WorkspaceLookOffset.reset()
        }
    }

    @Test
    fun effectiveLook_usesLookOffsetOnlyWhenGlassesHeadTrackingActive() {
        GlassesSessionState.xrInputMode = GlassesXrInputMode.GLASSES_HEAD_TRACKING
        WorkspaceLookOffset.setOffset(12f, 8f)
        try {
            val appearance = WorkspaceAppearance(lookYawDegrees = 20f, lookPitchDegrees = 5f)
            assertEquals(12f, WorkspaceWraparound.effectiveLookYaw(appearance), 0.001f)
            assertEquals(8f, WorkspaceWraparound.effectiveLookPitch(appearance), 0.001f)
        } finally {
            WorkspaceLookOffset.reset()
            GlassesSessionState.xrInputMode = GlassesXrInputMode.COMPANION
        }
    }
}
