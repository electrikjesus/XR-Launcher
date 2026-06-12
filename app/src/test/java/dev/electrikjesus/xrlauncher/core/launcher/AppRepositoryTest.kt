package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRepositoryTest {
    private val apps = listOf(
        LaunchableApp("Browser", android.content.ComponentName("com.test", ".Browser"), "com.test"),
        LaunchableApp("Calendar", android.content.ComponentName("com.cal", ".Main"), "com.cal"),
        LaunchableApp("Camera", android.content.ComponentName("com.cam", ".Main"), "com.cam"),
    )

    @Test
    fun filterApps_emptyQueryReturnsAll() {
        assertEquals(3, AppRepository.filterLaunchableApps(apps, "").size)
    }

    @Test
    fun filterApps_matchesLabel() {
        val result = AppRepository.filterLaunchableApps(apps, "cal")
        assertEquals(1, result.size)
        assertEquals("Calendar", result.first().label)
    }

    @Test
    fun filterApps_matchesPackage() {
        assertEquals(1, AppRepository.filterLaunchableApps(apps, "com.cam").size)
    }
}
