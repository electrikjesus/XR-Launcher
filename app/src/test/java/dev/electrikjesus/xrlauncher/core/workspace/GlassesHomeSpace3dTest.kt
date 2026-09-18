package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GlassesHomeSpace3dTest {
    @Test
    fun focusedPane_sitsInFrontOfTheCamera() {
        val pose = GlassesHomeSpace3d.paneWorldPose(0f)
        assertEquals(0f, pose.x, 0.001f)
        assertEquals(-GlassesHomeSpace3d.PANE_RADIUS, pose.z, 0.001f)
        assertEquals(0f, pose.rotationYDeg, 0.001f)
    }

    @Test
    fun rightPane_hasPositiveXAndFacesTheCenter() {
        val pose = GlassesHomeSpace3d.paneWorldPose(1f)
        assertTrue(pose.x > 0f)
        assertTrue(pose.z < 0f)
        assertTrue(pose.rotationYDeg < 0f)
        assertTrue(abs(pose.z) < GlassesHomeSpace3d.PANE_RADIUS)
    }

    @Test
    fun project_focusedPaneIsCenteredAndFaceOn() {
        val projected = GlassesHomeSpace3d.projectPane(
            worldX = 0f,
            look = 0f,
            cursorX = 0.5f,
            cursorY = 0.5f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(projected.visible)
        assertEquals(0f, projected.translationXPx, 2f)
        assertEquals(0f, projected.translationYPx, 2f)
        assertEquals(0f, projected.rotationYDeg, 0.2f)
        assertEquals(1f, projected.scale, 0.02f)
    }

    @Test
    fun project_rightPaneSitsToTheRightWithoutSpinning() {
        val projected = GlassesHomeSpace3d.projectPane(
            worldX = 1f,
            look = 0f,
            cursorX = 0.5f,
            cursorY = 0.5f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(projected.visible)
        assertTrue(projected.translationXPx > 700f)
        assertEquals(0f, projected.rotationYDeg, 0.2f)
        assertTrue(projected.viewZ > -GlassesHomeSpace3d.PANE_RADIUS)
    }

    @Test
    fun lookingRight_centersTheRightPaneAndSlidesHomeLeft() {
        val right = GlassesHomeSpace3d.projectPane(
            worldX = 1f,
            look = 1f,
            cursorX = 0.5f,
            cursorY = 0.5f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        val home = GlassesHomeSpace3d.projectPane(
            worldX = 0f,
            look = 1f,
            cursorX = 0.5f,
            cursorY = 0.5f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(right.visible)
        assertEquals(0f, right.translationXPx, 8f)
        assertEquals(0f, right.rotationYDeg, 0.5f)
        assertTrue(home.translationXPx < -700f)
        assertEquals(0f, home.rotationYDeg, 0.5f)
    }

    @Test
    fun lookingDown_shiftsTheFrontPaneUp() {
        val projected = GlassesHomeSpace3d.projectPane(
            worldX = 0f,
            look = 0f,
            cursorX = 0.5f,
            cursorY = 0.9f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(projected.visible)
        assertTrue(projected.translationYPx < -80f)
        assertEquals(0f, projected.rotationXDeg, 0.2f)
        val lookingUp = GlassesHomeSpace3d.projectPane(
            worldX = 0f,
            look = 0f,
            cursorX = 0.5f,
            cursorY = 0.1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(lookingUp.translationYPx > 80f)
        assertEquals(0f, lookingUp.rotationXDeg, 0.2f)
    }

    @Test
    fun roomSitsFartherThanPanes() {
        assertTrue(GlassesHomeSpace3d.ROOM_RADIUS > GlassesHomeSpace3d.PANE_RADIUS)
    }
}
