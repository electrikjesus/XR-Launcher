package dev.electrikjesus.xrlauncher.core.launcher

import android.view.Display
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppWidgetHostFeasibilityTest {
    @Test
    fun assess_secondaryDisplayFlagsVirtualDisplayBlocked() {
        val assessment = AppWidgetHostFeasibility.assess(displayId = 4)
        assertTrue(assessment.externalDisplaySupported)
        assertTrue(assessment.virtualDisplayBlocked)
    }

    @Test
    fun assess_defaultDisplay_isNotSecondary() {
        val assessment = AppWidgetHostFeasibility.assess(Display.DEFAULT_DISPLAY)
        assertFalse(assessment.externalDisplaySupported)
    }
}
