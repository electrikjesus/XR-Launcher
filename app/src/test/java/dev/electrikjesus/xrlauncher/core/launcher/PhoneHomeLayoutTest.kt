package dev.electrikjesus.xrlauncher.core.launcher

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneHomeLayoutTest {
    private val browser = LaunchableApp("Browser", ComponentName("com.browser", ".Main"), "com.browser")
    private val camera = LaunchableApp("Camera", ComponentName("com.cam", ".Main"), "com.cam")
    private val clock = LaunchableApp("Clock", ComponentName("com.clock", ".Main"), "com.clock")
    private val apps = listOf(browser, camera, clock)

    @Test
    fun gridOmitsHotseatApps() {
        val grid = PhoneHomeLayout.gridApps(apps, hotseatApps = listOf(browser), query = "")
        assertEquals(listOf(camera, clock), grid)
    }

    @Test
    fun gridAppliesSearch() {
        val grid = PhoneHomeLayout.gridApps(apps, hotseatApps = emptyList(), query = "cam")
        assertEquals(listOf(camera), grid)
    }

    @Test
    fun hotseatCapsAtFourSlots() {
        val extra = LaunchableApp("Maps", ComponentName("com.maps", ".Main"), "com.maps")
        val hotseat = PhoneHomeLayout.resolveHotseat(apps + extra, pinnedKeys = emptyList())
        assertEquals(PhoneHomeLayout.HOTSEAT_SLOTS, hotseat.size)
    }
}
