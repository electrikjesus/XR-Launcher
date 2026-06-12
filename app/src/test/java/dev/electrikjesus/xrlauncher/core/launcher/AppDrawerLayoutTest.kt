package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDrawerLayoutTest {
    private val apps = listOf(
        LaunchableApp("Browser", android.content.ComponentName("com.test", ".Browser"), "com.test"),
        LaunchableApp("Calendar", android.content.ComponentName("com.cal", ".Main"), "com.cal"),
        LaunchableApp("Camera", android.content.ComponentName("com.cam", ".Main"), "com.cam"),
        LaunchableApp("1Password", android.content.ComponentName("com.1p", ".Main"), "com.1p"),
    )

    @Test
    fun buildItems_emptyReturnsEmpty() {
        assertTrue(AppDrawerLayout.buildItems(emptyList()).isEmpty())
    }

    @Test
    fun buildItems_addsAlphabeticalHeaders() {
        val items = AppDrawerLayout.buildItems(apps)
        val headers = items.filterIsInstance<AppDrawerItem.SectionHeader>().map { it.letter }
        assertEquals(listOf('1', 'B', 'C'), headers)
    }

    @Test
    fun buildItems_whenFiltering_skipsHeaders() {
        val filtered = AppRepository.filterLaunchableApps(apps, "cal")
        val items = AppDrawerLayout.buildItems(filtered, query = "cal")
        assertEquals(1, items.size)
        assertTrue(items[0] is AppDrawerItem.AppEntry)
    }
}
