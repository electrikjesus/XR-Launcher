package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RadialMenuWorldLockTest {
    @Test
    fun projectToScreenPx_centerPose_staysNearViewportCenter() {
        val camera = HomeSpaceScene.Camera(yawDeg = 0f, pitchDeg = 0f)
        val px = RadialMenuWorldLock.projectToScreenPx(
            yawDeg = 0f,
            pitchDeg = 0f,
            camera = camera,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            sphereScale = 1f,
        )
        assertNotNull(px)
        assertEquals(960f, px!!.first, 8f)
        assertEquals(540f, px.second, 8f)
    }

    @Test
    fun projectToScreenPx_movesOppositeOfCameraYaw() {
        val lookingRight = HomeSpaceScene.Camera(yawDeg = 25f, pitchDeg = 0f)
        val px = RadialMenuWorldLock.projectToScreenPx(
            yawDeg = 0f,
            pitchDeg = 0f,
            camera = lookingRight,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            sphereScale = 1f,
        )
        assertNotNull(px)
        assertTrue(
            "world-locked menu should slide left when camera yaws right, x=${px!!.first}",
            px.first < 960f - 40f,
        )
    }

    @Test
    fun projectToScreenPx_behindCamera_returnsNull() {
        val camera = HomeSpaceScene.Camera(yawDeg = 0f, pitchDeg = 0f)
        val px = RadialMenuWorldLock.projectToScreenPx(
            yawDeg = 180f,
            pitchDeg = 0f,
            camera = camera,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            sphereScale = 1f,
        )
        assertNull(px)
    }

    @Test
    fun projectToScreenPx_matchesSphereHitUnderFpsCrosshair() {
        // Mouse-look: cursor stuck at center; opening at current look must project back to center.
        val lookYaw = -35f
        val lookPitch = 8f
        val camera = HomeSpaceScene.Camera(yawDeg = lookYaw, pitchDeg = lookPitch)
        val hit = HomeSpaceScene.sphereHit(
            cursorX = 0.5f,
            cursorY = 0.5f,
            camera = camera,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            sphereScale = 1f,
        )
        val px = RadialMenuWorldLock.projectToScreenPx(
            yawDeg = hit.yawDeg,
            pitchDeg = hit.pitchDeg,
            camera = camera,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            sphereScale = 1f,
        )
        assertNotNull(px)
        assertTrue(abs(px!!.first - 960f) < 12f)
        assertTrue(abs(px.second - 540f) < 12f)

        val lookingElsewhere = HomeSpaceScene.Camera(yawDeg = lookYaw + 40f, pitchDeg = lookPitch)
        val moved = RadialMenuWorldLock.projectToScreenPx(
            yawDeg = hit.yawDeg,
            pitchDeg = hit.pitchDeg,
            camera = lookingElsewhere,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            sphereScale = 1f,
        )
        assertNotNull(moved)
        assertTrue(
            "after looking away the menu must leave the crosshair, x=${moved!!.first}",
            abs(moved.first - 960f) > 80f,
        )
    }
}
