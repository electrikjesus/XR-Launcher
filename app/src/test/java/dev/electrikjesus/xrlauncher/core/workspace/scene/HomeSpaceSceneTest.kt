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
        assertEquals(0f, center.yawDeg, 0.2f)
        assertEquals(HomeSpaceScene.CURSOR_YAW_DEGREES, right.yawDeg, 0.2f)
        assertTrue(right.yawDeg >= 20f)
    }
}
