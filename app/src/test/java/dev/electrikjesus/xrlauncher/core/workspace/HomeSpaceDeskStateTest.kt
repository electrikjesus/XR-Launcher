package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.workspace.scene.HomeSpaceDesk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class HomeSpaceDeskStateTest {
    private val app = HomeSpaceDesk.AppRef("a/.Main", "Alpha", "a")

    @Before
    fun reset() {
        HomeSpaceDeskState.clear()
        AllAppsPaginationState.reset()
    }

    @Test
    fun clickWithoutPull_doesNotPlace() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.505f, 0.5f, yawDeg = -40f, pitchDeg = 0f)
        assertFalse(HomeSpaceDeskState.release(onDesktop = true))
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }

    @Test
    fun pullOntoEmptyDesktop_placesTheIcon() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals(app.componentKey, HomeSpaceDeskState.placed.first().app.componentKey)
        assertEquals(-12f, HomeSpaceDeskState.placed.first().yawDeg, 0.01f)
    }

    @Test
    fun pullOntoTheWidget_snapsBack() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = false))
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }

    @Test
    fun pullOntoAnotherIcon_separatesOrRejectsOverlap() {
        HomeSpaceDeskState.clear()
        val other = HomeSpaceDesk.AppRef("b/.Main", "Beta", "b")
        val obstacle = HomeSpaceDesk.iconOf(other, yawDeg = -12f, pitchDeg = 6f, sphereScale = 1f)
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(
            HomeSpaceDeskState.release(
                onDesktop = true,
                obstacles = listOf(obstacle),
            ),
        )
        assertEquals(1, HomeSpaceDeskState.placed.size)
        val placed = HomeSpaceDeskState.placed.first()
        assertTrue(
            abs(placed.yawDeg - obstacle.yawDeg) > 2f || abs(placed.pitchDeg - obstacle.pitchDeg) > 2f,
        )
    }

    @Test
    fun pullOntoOpenWidgetBacking_rejects() {
        val backing = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef(HomeSpaceDesk.BACKING_KEY, "All apps", "", HomeSpaceDesk.Kind.DRAWER_BACKING),
            yawDeg = -40f,
            pitchDeg = 0f,
            sphereScale = 1f,
            halfWidth = 0.3f,
            halfHeight = 0.35f,
            lift = 0.11f,
        )
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -10f, pitchDeg = 10f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.55f, 0.45f, yawDeg = -40f, pitchDeg = 0f)
        assertTrue(
            HomeSpaceDeskState.release(
                onDesktop = true,
                obstacles = listOf(backing),
            ),
        )
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }

    @Test
    fun notePointerUp_marksPullingAndConsumesClick() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        assertTrue(HomeSpaceDeskState.notePointerUp(cursorMoved = true))
        assertTrue(HomeSpaceDeskState.drag!!.pulling)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(1, HomeSpaceDeskState.placed.size)
    }

    @Test
    fun notePointerUp_withoutMove_allowsClick() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        assertFalse(HomeSpaceDeskState.notePointerUp(cursorMoved = false))
        assertFalse(HomeSpaceDeskState.release(onDesktop = true))
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }

    @Test
    fun notePointerUp_firesPendingPagerChrome() {
        val prev = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef(HomeSpaceDesk.PAGE_PREV_KEY, "Previous", "", HomeSpaceDesk.Kind.PAGE_PREV),
            yawDeg = -40f,
            pitchDeg = -8f,
            sphereScale = 1f,
        )
        AllAppsPaginationState.updatePageCount(40, HomeSpaceDesk.DRAWER_PAGE_SIZE)
        AllAppsPaginationState.goToPage(2)
        HomeSpaceDeskState.press(prev, 0.5f, 0.5f)
        assertTrue(HomeSpaceDeskState.notePointerUp(cursorMoved = false))
        assertEquals(1, AllAppsPaginationState.pageIndex)
    }

    @Test
    fun notePointerUp_allAppsChromeClearsDrag() {
        val drawer = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef(
                HomeSpaceDesk.DRAWER_KEY,
                HomeSpaceDesk.DRAWER_LABEL,
                "",
                HomeSpaceDesk.Kind.APP_DRAWER,
            ),
            yawDeg = -40f,
            pitchDeg = 0f,
            sphereScale = 1f,
        )
        HomeSpaceDeskState.press(drawer, 0.5f, 0.5f)
        assertTrue(HomeSpaceDeskState.drag != null)
        assertTrue(HomeSpaceDeskState.notePointerUp(cursorMoved = false))
        assertTrue(HomeSpaceDeskState.drag == null)
    }

    @Test
    fun pullAllAppsTile_reposesDrawer() {
        val drawer = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef(
                HomeSpaceDesk.DRAWER_KEY,
                HomeSpaceDesk.DRAWER_LABEL,
                "",
                HomeSpaceDesk.Kind.APP_DRAWER,
            ),
            yawDeg = -40f,
            pitchDeg = 0f,
            sphereScale = 1f,
        )
        HomeSpaceDeskState.press(drawer, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(-12f, HomeSpaceDeskState.drawerPose!!.first, 0.01f)
        assertEquals(6f, HomeSpaceDeskState.drawerPose!!.second, 0.01f)
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }

    @Test
    fun releaseAfterPull_placesAtRestWithoutImpulse() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        val placed = HomeSpaceDeskState.placed.first()
        assertEquals(0f, placed.velYawDeg, 0.001f)
        assertEquals(0f, placed.velPitchDeg, 0.001f)
    }

    @Test
    fun fpsStyleLookMove_pullsWithoutCursorTravel() {
        val icon = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        // Off-center grab: hit angles differ from icon center but must not instantly pull.
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f, hitYawDeg = -38f, hitPitchDeg = 1f)
        HomeSpaceDeskState.move(0.5f, 0.5f, yawDeg = -38f, pitchDeg = 1f)
        assertFalse(HomeSpaceDeskState.drag!!.pulling)
        // Gaze moves (cursor stays centered) — same as FPS mouse-look while Hold-Left.
        HomeSpaceDeskState.move(0.5f, 0.5f, yawDeg = -20f, pitchDeg = 8f)
        assertTrue(HomeSpaceDeskState.drag!!.pulling)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals(-20f, HomeSpaceDeskState.placed.first().yawDeg, 0.01f)
    }

    @Test
    fun pullFromHome_placesDesktopCopy() {
        val icon = HomeSpaceDesk.iconOf(
            app,
            yawDeg = -10f,
            pitchDeg = 5f,
            sphereScale = 1f,
            lift = 0.15f,
        )
        HomeSpaceDeskState.press(icon, 0.5f, 0.5f, hitYawDeg = -10f, hitPitchDeg = 5f, fromHome = true)
        assertTrue(HomeSpaceDeskState.drag!!.fromHome)
        HomeSpaceDeskState.move(0.6f, 0.45f, yawDeg = -25f, pitchDeg = 8f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(1, HomeSpaceDeskState.placed.size)
        assertEquals(app.componentKey, HomeSpaceDeskState.placed.first().app.componentKey)
        assertTrue(HomeSpaceDeskState.drag == null)
    }

    @Test
    fun dragDesktopIconOntoAllAppsTile_removesIt() {
        val drawer = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef(
                HomeSpaceDesk.DRAWER_KEY,
                HomeSpaceDesk.DRAWER_LABEL,
                "",
                HomeSpaceDesk.Kind.APP_DRAWER,
            ),
            yawDeg = -40f,
            pitchDeg = 0f,
            sphereScale = 1f,
            halfWidth = HomeSpaceDesk.ICON_HALF_WIDTH * HomeSpaceDesk.DRAWER_SCALE,
            halfHeight = HomeSpaceDesk.ICON_HALF_HEIGHT * HomeSpaceDesk.DRAWER_SCALE,
        )
        val fromDrawer = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(fromDrawer, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(1, HomeSpaceDeskState.placed.size)

        val deskIcon = HomeSpaceDesk.iconOf(app, yawDeg = -12f, pitchDeg = 6f, sphereScale = 1f)
        HomeSpaceDeskState.press(deskIcon, 0.5f, 0.5f, hitYawDeg = -12f, hitPitchDeg = 6f)
        HomeSpaceDeskState.move(0.55f, 0.5f, yawDeg = -40f, pitchDeg = 0f)
        assertTrue(
            HomeSpaceDeskState.release(
                onDesktop = true,
                obstacles = listOf(drawer),
            ),
        )
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }

    @Test
    fun dragDesktopIconOntoOpenBacking_removesIt() {
        val backing = HomeSpaceDesk.iconOf(
            HomeSpaceDesk.AppRef(HomeSpaceDesk.BACKING_KEY, "All apps", "", HomeSpaceDesk.Kind.DRAWER_BACKING),
            yawDeg = -40f,
            pitchDeg = 0f,
            sphereScale = 1f,
            halfWidth = 0.3f,
            halfHeight = 0.35f,
            lift = 0.11f,
        )
        val fromDrawer = HomeSpaceDesk.iconOf(app, yawDeg = -40f, pitchDeg = 0f, sphereScale = 1f, lift = 0.15f)
        HomeSpaceDeskState.press(fromDrawer, 0.5f, 0.5f)
        HomeSpaceDeskState.move(0.7f, 0.4f, yawDeg = -12f, pitchDeg = 6f)
        assertTrue(HomeSpaceDeskState.release(onDesktop = true))
        assertEquals(1, HomeSpaceDeskState.placed.size)

        val deskIcon = HomeSpaceDesk.iconOf(app, yawDeg = -12f, pitchDeg = 6f, sphereScale = 1f)
        HomeSpaceDeskState.press(deskIcon, 0.5f, 0.5f, hitYawDeg = -12f, hitPitchDeg = 6f)
        HomeSpaceDeskState.move(0.55f, 0.5f, yawDeg = -40f, pitchDeg = 0f)
        assertTrue(
            HomeSpaceDeskState.release(
                onDesktop = true,
                obstacles = listOf(backing),
            ),
        )
        assertTrue(HomeSpaceDeskState.placed.isEmpty())
    }
}
