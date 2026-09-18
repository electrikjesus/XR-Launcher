package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GlassesHomeHitsTest {
    @Before
    fun resetRecents() {
        GlassesRecentApps.clear()
    }

    @Test
    fun hoverLabel_mapsKnownKeys() {
        assertEquals("Recents", GlassesHomeHits.hoverLabel(GlassesHomeHits.RECENTS))
        assertEquals("Clear all", GlassesHomeHits.hoverLabel(GlassesHomeHits.RECENTS_CLEAR))
        assertEquals("Close", GlassesHomeHits.hoverLabel(GlassesHomeHits.appCloseKey("app_one")))
    }

    @Test
    fun actionKeyAt_prefersCloseThenChrome() {
        val hit = GlassesHomeHits.actionKeyAt { it == GlassesHomeHits.ALL_APPS }
        assertEquals(GlassesHomeHits.ALL_APPS, hit)
    }

    @Test
    fun appClosePanelId_roundTrips() {
        val key = GlassesHomeHits.appCloseKey("app_maps")
        assertEquals("app_maps", GlassesHomeHits.appClosePanelId(key))
        assertEquals(null, GlassesHomeHits.appClosePanelId(GlassesHomeHits.HOME))
    }

    @Test
    fun recents_recordDedupesAndCaps() {
        val first = sampleApp("one")
        val second = sampleApp("two")
        GlassesRecentApps.record(first)
        GlassesRecentApps.record(second)
        GlassesRecentApps.record(first)
        val list = GlassesRecentApps.list()
        assertEquals("one", list.first().packageName)
        assertEquals(2, list.size)
    }

    @Test
    fun recents_seedOnlyWhenEmpty() {
        val seed = listOf(sampleApp("a"), sampleApp("b"))
        GlassesRecentApps.seedIfEmpty(seed)
        GlassesRecentApps.seedIfEmpty(listOf(sampleApp("c")))
        assertEquals(2, GlassesRecentApps.list().size)
        assertEquals("a", GlassesRecentApps.list().first().packageName)
    }

    @Test
    fun recents_clearEmpties() {
        GlassesRecentApps.record(sampleApp("x"))
        GlassesRecentApps.clear()
        assertTrue(GlassesRecentApps.list().isEmpty())
    }

    private fun sampleApp(pkg: String) = LaunchableApp(
        label = pkg,
        componentName = android.content.ComponentName(pkg, "$pkg.Main"),
        packageName = pkg,
    )
}
