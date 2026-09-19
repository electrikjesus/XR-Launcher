package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HomeSpaceSceneTest {
    @Test
    fun paneCorners_sitOnTheInvisibleSphere() {
        val pane = HomeSpaceScene.pane(0f, 1920f, 1080f)
        pane.corners.forEach { corner ->
            assertEquals(HomeSpaceScene.SPHERE_RADIUS, corner.length(), 0.02f)
        }
        assertTrue(pane.center.length() < HomeSpaceScene.SPHERE_RADIUS)
    }

    @Test
    fun paneCorners_scaledSphere_sitOnScaledRadius() {
        val scale = 1.2f
        val pane = HomeSpaceScene.pane(0f, 1920f, 1080f, sphereScale = scale)
        val radius = HomeSpaceScene.sphereRadius(scale)
        pane.corners.forEach { corner ->
            assertEquals(radius, corner.length(), 0.02f)
        }
    }

    @Test
    fun packedPanes_doNotOverlapOnTheSphere() {
        val hits = HomeSpaceScene.overlappingPairs(
            worldXs = listOf(-1f, 0f, 1f),
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
        )
        assertTrue(hits.isEmpty())
    }

    @Test
    fun adjacentPanes_keepAGapBetweenYawSpans() {
        val home = HomeSpaceScene.pane(0f, 1920f, 1080f)
        val right = HomeSpaceScene.pane(1f, 1920f, 1080f)
        assertTrue(right.yawMin - home.yawMax >= HomeSpaceScene.PANE_GAP_DEGREES - 0.2f)
    }

    @Test
    fun largerSphereScale_placesThePaneFartherAndSmallerOnScreen() {
        val near = HomeSpaceScene.pane(0f, 1920f, 1080f, panelScale = 1f, sphereScale = 1f)
        val far = HomeSpaceScene.pane(0f, 1920f, 1080f, panelScale = 1f, sphereScale = 1.6f)
        assertTrue(far.center.length() > near.center.length() + 0.4f)
        assertTrue(
            "farther sphere must shrink the pane's angular size, near=${near.halfWidthDeg} far=${far.halfWidthDeg}",
            far.halfWidthDeg < near.halfWidthDeg - 4f,
        )
    }

    @Test
    fun lookingRight_isAnFpsCameraTurnNotABillboard() {
        val camera = HomeSpaceScene.camera(1f, 0.5f, 0.5f, 1920f, 1080f)
        val home = HomeSpaceScene.pane(0f, 1920f, 1080f)
        val view = camera.viewPoint(home.center)
        assertTrue(view.x < 0f)
        assertTrue(abs(camera.yawDeg) > 40f)
    }

    @Test
    fun cursorAtRightEdge_yawsTheFpsCamera() {
        val center = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val right = HomeSpaceScene.camera(0f, 1f, 0.5f, 1920f, 1080f)
        val down = HomeSpaceScene.camera(0f, 0.5f, 1f, 1920f, 1080f)
        assertEquals(0f, center.yawDeg, 0.2f)
        assertEquals(HomeSpaceScene.CURSOR_YAW_DEGREES, right.yawDeg, 0.2f)
        assertEquals(HomeSpaceScene.CURSOR_PITCH_DEGREES, down.pitchDeg, 0.2f)
        assertTrue(right.yawDeg >= 20f)
        assertTrue(down.pitchDeg >= 16f)
    }

    @Test
    fun fpsLook_ignoresCursorOffsetAndUsesLookPitch() {
        val fps = HomeSpaceScene.camera(
            look = 0f,
            cursorX = 1f,
            cursorY = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            lookMode = dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode.FPS,
            lookPitchDeg = 8f,
        )
        assertEquals(0f, fps.yawDeg, 0.2f)
        assertEquals(8f, fps.pitchDeg, 0.2f)
    }

    @Test
    fun absoluteHostCursor_ignoresGradientCursorOffset() {
        val cam = HomeSpaceScene.camera(
            look = 0f,
            cursorX = 1f,
            cursorY = 1f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            lookMode = dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode.GRADIENT,
            lookPitchDeg = 5f,
            applyCursorOffset = false,
        )
        assertEquals(0f, cam.yawDeg, 0.2f)
        assertEquals(5f, cam.pitchDeg, 0.2f)
    }

    @Test
    fun freeLook_usesLookYawDegreesDirectly() {
        val cam = HomeSpaceScene.camera(
            look = 0f,
            cursorX = 0.5f,
            cursorY = 0.5f,
            viewportWidthPx = 1920f,
            viewportHeightPx = 1080f,
            lookMode = dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode.FPS,
            lookPitchDeg = 0f,
            lookYawDegrees = 270f,
        )
        assertEquals(270f, cam.yawDeg, 0.2f)
    }
}
