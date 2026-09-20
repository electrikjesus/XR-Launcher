package dev.electrikjesus.xrlauncher.core.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForeignWindowInjectLogicTest {
    private val ourPkg = "dev.electrikjesus.xrlauncher"

    @Test
    fun pipWindow_underCursor_injects() {
        val pip = ForeignWindowInjectLogic.WindowHit(
            displayId = 68,
            type = ForeignWindowInjectLogic.TYPE_APPLICATION,
            left = 100,
            top = 100,
            right = 400,
            bottom = 400,
            isPictureInPicture = true,
            packageName = "com.example.video",
        )
        assertTrue(
            ForeignWindowInjectLogic.shouldInjectAt(
                displayId = 68,
                xPx = 200,
                yPx = 200,
                ourPackageName = ourPkg,
                windows = listOf(pip),
            ),
        )
    }

    @Test
    fun ourLauncherWindow_underCursor_doesNotInject() {
        val home = ForeignWindowInjectLogic.WindowHit(
            displayId = 68,
            type = ForeignWindowInjectLogic.TYPE_APPLICATION,
            left = 0,
            top = 0,
            right = 1920,
            bottom = 1080,
            isPictureInPicture = false,
            packageName = ourPkg,
        )
        assertFalse(
            ForeignWindowInjectLogic.shouldInjectAt(
                displayId = 68,
                xPx = 960,
                yPx = 540,
                ourPackageName = ourPkg,
                windows = listOf(home),
            ),
        )
    }

    @Test
    fun foreignApp_underCursor_injects() {
        val other = ForeignWindowInjectLogic.WindowHit(
            displayId = 68,
            type = ForeignWindowInjectLogic.TYPE_APPLICATION,
            left = 0,
            top = 0,
            right = 800,
            bottom = 600,
            isPictureInPicture = false,
            packageName = "com.android.settings",
        )
        assertTrue(
            ForeignWindowInjectLogic.shouldInjectAt(
                displayId = 68,
                xPx = 100,
                yPx = 100,
                ourPackageName = ourPkg,
                windows = listOf(other),
            ),
        )
    }

    @Test
    fun accessibilityOverlay_ignored() {
        val overlay = ForeignWindowInjectLogic.WindowHit(
            displayId = 68,
            type = ForeignWindowInjectLogic.TYPE_ACCESSIBILITY_OVERLAY,
            left = 0,
            top = 0,
            right = 1920,
            bottom = 1080,
            isPictureInPicture = false,
            packageName = ourPkg,
        )
        assertFalse(
            ForeignWindowInjectLogic.shouldInjectAt(
                displayId = 68,
                xPx = 10,
                yPx = 10,
                ourPackageName = ourPkg,
                windows = listOf(overlay),
            ),
        )
    }

    @Test
    fun missOutsideBounds_doesNotInject() {
        val pip = ForeignWindowInjectLogic.WindowHit(
            displayId = 68,
            type = ForeignWindowInjectLogic.TYPE_APPLICATION,
            left = 100,
            top = 100,
            right = 200,
            bottom = 200,
            isPictureInPicture = true,
            packageName = "com.example.video",
        )
        assertFalse(
            ForeignWindowInjectLogic.shouldInjectAt(
                displayId = 68,
                xPx = 50,
                yPx = 50,
                ourPackageName = ourPkg,
                windows = listOf(pip),
            ),
        )
    }
}
