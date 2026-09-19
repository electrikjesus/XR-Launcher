package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GlassesLookModeTest {
    @Before
    fun reset() {
        GlassesLookMode.preference = GlassesLookMode.GRADIENT
        GlassesSessionState.markLauncherForeground()
    }

    @Test
    fun fromPersisted_readsFps() {
        assertEquals(GlassesLookMode.FPS, GlassesLookMode.fromPersisted("fps"))
        assertEquals(GlassesLookMode.GRADIENT, GlassesLookMode.fromPersisted("gradient"))
        assertEquals(GlassesLookMode.GRADIENT, GlassesLookMode.fromPersisted(null))
    }

    @Test
    fun effective_usesGradientWhenAnAppIsInFront() {
        GlassesLookMode.preference = GlassesLookMode.FPS
        GlassesSessionState.markLauncherForeground()
        GlassesSessionState.hostImmersiveSession = false
        assertEquals(GlassesLookMode.FPS, GlassesLookMode.effective())
        GlassesSessionState.launcherForeground = false
        assertEquals(GlassesLookMode.GRADIENT, GlassesLookMode.effective())
    }

    @Test
    fun effective_keepsFpsDuringHostImmersiveEvenIfForegroundCleared() {
        GlassesLookMode.preference = GlassesLookMode.FPS
        GlassesSessionState.launcherForeground = false
        GlassesSessionState.hostImmersiveSession = true
        assertEquals(GlassesLookMode.FPS, GlassesLookMode.effective())
        GlassesSessionState.hostImmersiveSession = false
        assertEquals(GlassesLookMode.GRADIENT, GlassesLookMode.effective())
    }
}
