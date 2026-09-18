package dev.electrikjesus.xrlauncher.core.workspace.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSpaceDeskTest {
    private val apps = listOf(
        HomeSpaceDesk.AppRef("a/.Main", "Alpha", "a"),
        HomeSpaceDesk.AppRef("b/.Main", "Beta", "b"),
        HomeSpaceDesk.AppRef("c/.Main", "Gamma", "c"),
    )

    @Test
    fun lookingLeft_facesTheDesktopOnTheSphere() {
        val home = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val desk = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        assertEquals(0f, home.pitchDeg, 0.2f)
        assertEquals(0f, desk.pitchDeg, 0.2f)
        assertTrue(desk.yawDeg < -20f)
        assertEquals(HomeSpaceDesk.yawDegrees(1920f, 1080f), desk.yawDeg, 0.2f)
    }

    @Test
    fun lookingLeft_centerRayPicksTheAllAppsTile() {
        val camera = HomeSpaceScene.camera(-1f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceScene.sphereHit(0.5f, 0.5f, camera, 1920f, 1080f)
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        assertEquals(1, icons.size)
        assertTrue(icons.first().isAppDrawer)
        assertEquals(
            HomeSpaceDesk.DRAWER_KEY,
            HomeSpaceDesk.pickIcon(hit.world, icons)!!.componentKey,
        )
    }

    @Test
    fun defaultIcons_areThinPancakesOnTheSphere() {
        assertTrue(HomeSpaceDesk.ICON_HALF_THICK * 4f < HomeSpaceDesk.ICON_HALF_WIDTH)
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        assertTrue(drawer.halfThick * 4f < drawer.halfWidth)
        assertEquals(
            HomeSpaceScene.innerSphereRadius(1f),
            drawer.center.length(),
            0.04f,
        )
    }

    @Test
    fun layout_keepsTheDrawerWhenPlacingApps() {
        val icons = HomeSpaceDesk.layout(apps, 1f, 1920f, 1080f)
        assertEquals(1 + apps.size, icons.size)
        assertTrue(icons.first().isAppDrawer)
        assertTrue(icons[1].yawDeg != icons[2].yawDeg || icons[1].pitchDeg != icons[2].pitchDeg)
    }

    @Test
    fun pickIcon_hitsTheIconUnderTheRay() {
        val icons = HomeSpaceDesk.layout(apps, 1f, 1920f, 1080f)
        val first = icons.first()
        val picked = HomeSpaceDesk.pickIcon(first.center, icons)
        assertEquals(first.componentKey, picked!!.componentKey)
        assertNull(HomeSpaceDesk.pickIcon(Vec3(0f, 0f, -1f), icons))
    }

    @Test
    fun pickIcon_usesIconTangentAxes() {
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        val drawer = icons.first()
        val alongRight = HomeSpaceDesk.rightAxis(drawer.yawDeg)
        val miss = Vec3(
            drawer.center.x + alongRight.x * drawer.halfWidth * 2.2f,
            drawer.center.y,
            drawer.center.z + alongRight.z * drawer.halfWidth * 2.2f,
        )
        assertNull(HomeSpaceDesk.pickIcon(miss, icons))
        val hit = Vec3(
            drawer.center.x + alongRight.x * drawer.halfWidth * 0.4f,
            drawer.center.y,
            drawer.center.z + alongRight.z * drawer.halfWidth * 0.4f,
        )
        assertEquals(drawer.componentKey, HomeSpaceDesk.pickIcon(hit, icons)!!.componentKey)
    }

    @Test
    fun lookingHome_centerRayDoesNotPickTheDrawer() {
        val camera = HomeSpaceScene.camera(0f, 0.5f, 0.5f, 1920f, 1080f)
        val hit = HomeSpaceScene.sphereHit(0.5f, 0.5f, camera, 1920f, 1080f)
        val icons = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f)
        assertNull(HomeSpaceDesk.pickIcon(hit.world, icons))
    }

    @Test
    fun largerSphere_movesIconsFartherFromTheCamera() {
        val near = HomeSpaceDesk.pointOnSphere(-40f, 0f, 1f)
        val far = HomeSpaceDesk.pointOnSphere(-40f, 0f, 1.6f)
        assertTrue(far.length() > near.length() + 0.4f)
    }

    @Test
    fun hoverPad_isLargerThanTheIcon() {
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        val pad = HomeSpaceDesk.hoverPadMesh(drawer)
        assertEquals(6, pad.vertexCount)
        assertEquals(-2f, pad.interleaved[6], 0.001f)
        val alongRight = HomeSpaceDesk.rightAxis(drawer.yawDeg)
        val locals = (0 until pad.vertexCount).map { i ->
            val p = pad.position(i)
            (p.x - drawer.center.x) * alongRight.x + (p.z - drawer.center.z) * alongRight.z
        }
        assertTrue(locals.maxOrNull()!! - locals.minOrNull()!! > drawer.halfWidth * 2f)
    }

    @Test
    fun moved_reposesAnIconOnTheSphere() {
        val drawer = HomeSpaceDesk.defaultIcons(1f, 1920f, 1080f).first()
        val moved = HomeSpaceDesk.moved(drawer, yawDeg = 10f, pitchDeg = -8f, sphereScale = 1f)
        assertEquals(10f, moved.yawDeg, 0.01f)
        assertEquals(-8f, moved.pitchDeg, 0.01f)
        assertEquals(drawer.componentKey, moved.componentKey)
    }
}
