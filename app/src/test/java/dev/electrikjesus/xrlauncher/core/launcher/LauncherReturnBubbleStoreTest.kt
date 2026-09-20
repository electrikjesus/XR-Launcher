package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherReturnBubbleStoreTest {
    @Test
    fun defaults_areLargerThanOriginalHardCodedBubble() {
        assertEquals(80f, LauncherReturnBubbleStore.DEFAULT_SIZE_DP, 0.001f)
        assertEquals(48f, LauncherReturnBubbleStore.MIN_SIZE_DP, 0.001f)
        assertEquals(128f, LauncherReturnBubbleStore.MAX_SIZE_DP, 0.001f)
        assertTrue(LauncherReturnBubbleStore.DEFAULT_SIZE_DP > 56f)
    }
}
