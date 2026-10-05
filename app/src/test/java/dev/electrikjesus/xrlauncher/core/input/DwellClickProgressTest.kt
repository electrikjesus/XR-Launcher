package dev.electrikjesus.xrlauncher.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DwellClickProgressTest {
    @Test
    fun fillsAndFiresAfterDwellMs() {
        val p = DwellClickProgress(dwellMs = 1000L, cooldownMs = 300L)
        assertFalse(p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 400f))
        assertEquals(0.4f, p.progress, 0.001f)
        assertFalse(p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 400f))
        assertEquals(0.8f, p.progress, 0.001f)
        assertTrue(p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 300f))
        assertEquals(0f, p.progress, 0.001f)
    }

    @Test
    fun targetChangeResetsProgress() {
        val p = DwellClickProgress(dwellMs = 1000L)
        p.tick(active = true, targetKey = "a", pressed = false, deltaMs = 500f)
        assertEquals(0.5f, p.progress, 0.001f)
        p.tick(active = true, targetKey = "b", pressed = false, deltaMs = 100f)
        assertEquals(0.1f, p.progress, 0.001f)
    }

    @Test
    fun pressCancelsFill() {
        val p = DwellClickProgress(dwellMs = 1000L)
        p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 700f)
        assertFalse(p.tick(active = true, targetKey = "home", pressed = true, deltaMs = 100f))
        assertEquals(0f, p.progress, 0.001f)
    }

    @Test
    fun inactiveOrNullTargetClears() {
        val p = DwellClickProgress(dwellMs = 1000L)
        p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 500f)
        assertFalse(p.tick(active = false, targetKey = "home", pressed = false, deltaMs = 100f))
        assertEquals(0f, p.progress, 0.001f)
        p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 500f)
        assertFalse(p.tick(active = true, targetKey = null, pressed = false, deltaMs = 100f))
        assertEquals(0f, p.progress, 0.001f)
    }

    @Test
    fun cooldownBlocksImmediateRefire() {
        val p = DwellClickProgress(dwellMs = 100L, cooldownMs = 200L)
        assertTrue(p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 100f))
        assertFalse(p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 100f))
        assertEquals(0f, p.progress, 0.001f)
        assertFalse(p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 100f))
        // After cooldown, fill starts again
        assertFalse(p.tick(active = true, targetKey = "home", pressed = false, deltaMs = 50f))
        assertEquals(0.5f, p.progress, 0.001f)
    }
}
