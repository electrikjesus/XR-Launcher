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
    fun project_rightPaneSitsToTheRightAndTiltsInward() {
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
        assertTrue(projected.rotationYDeg < -40f)
        assertTrue(projected.scale < 1f)
        assertTrue(projected.viewZ > -GlassesHomeSpace3d.PANE_RADIUS)
    }

    @Test
    fun lookingRight_centersTheRightPane() {
        val projected = GlassesHomeSpace3d.projectPane(
            worldX = 1f,
            look = 1f,
            cursorX = 0.5f,
            cursorY = 0.5f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(projected.visible)
        assertEquals(0f, projected.translationXPx, 8f)
        assertEquals(0f, projected.rotationYDeg, 0.5f)
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
        assertTrue(projected.rotationXDeg < -15f)
        val lookingUp = GlassesHomeSpace3d.projectPane(
            worldX = 0f,
            look = 0f,
            cursorX = 0.5f,
            cursorY = 0.1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(lookingUp.translationYPx > 80f)
        assertTrue(lookingUp.rotationXDeg > 15f)
    }

    @Test
    fun roomSitsFartherThanPanes() {
        assertTrue(GlassesHomeSpace3d.ROOM_RADIUS > GlassesHomeSpace3d.PANE_RADIUS)
    }
}
