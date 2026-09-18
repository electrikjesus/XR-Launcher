package dev.electrikjesus.xrlauncher.core.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllAppsOverlayHitsTest {
    @Test
    fun hiddenOverlayNeverDismisses() {
        assertFalse(
            AllAppsOverlayHits.shouldDismiss(
                overlayVisible = false,
                hitClose = true,
                hitOtherTarget = false,
            ),
        )
    }

    @Test
    fun closeControlDismisses() {
        assertTrue(
            AllAppsOverlayHits.shouldDismiss(
                overlayVisible = true,
                hitClose = true,
                hitOtherTarget = false,
            ),
        )
    }

    @Test
    fun closeWinsOverOtherTargets() {
        assertTrue(
            AllAppsOverlayHits.shouldDismiss(
                overlayVisible = true,
                hitClose = true,
                hitOtherTarget = true,
            ),
        )
    }

    @Test
    fun emptyScrimDismisses() {
        assertTrue(
            AllAppsOverlayHits.shouldDismiss(
                overlayVisible = true,
                hitClose = false,
                hitOtherTarget = false,
            ),
        )
    }

    @Test
    fun appOrPaginationHitKeepsOverlayOpen() {
        assertFalse(
            AllAppsOverlayHits.shouldDismiss(
                overlayVisible = true,
                hitClose = false,
                hitOtherTarget = true,
            ),
        )
    }
}
