package dev.electrikjesus.xrlauncher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeSpacePaneNavTest {
    private val noPlanes = HomeSpacePaneNav.stops(emptyList())
    private val cycle = 5f

    @Test
    fun stops_runDesktopHomeAppsTray() {
        val planes = listOf(GlassesAppPlane("p1", "a/.Main", "A"), GlassesAppPlane("p2", "b/.Main", "B"))
        assertEquals(listOf(-1f, 0f), noPlanes.take(2))
        assertEquals(listOf(-1f, 0f, 1f, 2f, 3f), HomeSpacePaneNav.stops(planes))
    }

    @Test
    fun fromHome_stepsToTrayAndDesktop() {
        assertEquals(1f, HomeSpacePaneNav.neighborTarget(0f, noPlanes, 1, cycle)!!, 0.001f)
        assertEquals(-1f, HomeSpacePaneNav.neighborTarget(0f, noPlanes, -1, cycle)!!, 0.001f)
    }

    @Test
    fun ends_haveNoNeighbor() {
        assertNull(HomeSpacePaneNav.neighborTarget(1f, noPlanes, 1, cycle))
        assertNull(HomeSpacePaneNav.neighborTarget(-1f, noPlanes, -1, cycle))
    }

    @Test
    fun betweenPanes_goesToTheNextStopInThatDirection() {
        assertEquals(1f, HomeSpacePaneNav.neighborTarget(0.4f, noPlanes, 1, cycle)!!, 0.001f)
        assertEquals(0f, HomeSpacePaneNav.neighborTarget(0.4f, noPlanes, -1, cycle)!!, 0.001f)
    }

    @Test
    fun fullCircleLook_resolvesTheNearestTurn() {
        // One full turn right of Home: Tray is the next stop on that same turn.
        assertEquals(cycle + 1f, HomeSpacePaneNav.neighborTarget(cycle, noPlanes, 1, cycle)!!, 0.001f)
        assertEquals(-cycle - 1f, HomeSpacePaneNav.neighborTarget(-cycle, noPlanes, -1, cycle)!!, 0.001f)
    }

    @Test
    fun easing_hitsEndpoints() {
        assertEquals(0f, HomeSpacePaneNav.easeInOutCubic(0f), 0.0001f)
        assertEquals(0.5f, HomeSpacePaneNav.easeInOutCubic(0.5f), 0.0001f)
        assertEquals(1f, HomeSpacePaneNav.easeInOutCubic(1f), 0.0001f)
    }
}
