package dev.electrikjesus.xrlauncher.core.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingLogicTest {
    @Test
    fun showsOnFirstLaunchWithoutDisplay() {
        assertTrue(
            OnboardingLogic.shouldShow(
                completed = false,
                replayRequested = false,
                hasSecondaryDisplay = false,
            ),
        )
    }

    @Test
    fun hidesWhenCompleted() {
        assertFalse(
            OnboardingLogic.shouldShow(
                completed = true,
                replayRequested = false,
                hasSecondaryDisplay = false,
            ),
        )
    }

    @Test
    fun skipsWhenGlassesAlreadyConnected() {
        assertFalse(
            OnboardingLogic.shouldShow(
                completed = false,
                replayRequested = false,
                hasSecondaryDisplay = true,
            ),
        )
    }

    @Test
    fun replayOverridesCompletedAndDisplay() {
        assertTrue(
            OnboardingLogic.shouldShow(
                completed = true,
                replayRequested = true,
                hasSecondaryDisplay = true,
            ),
        )
    }

    @Test
    fun pagesIncludePermissionsWhenMissing() {
        val pages = OnboardingLogic.pages(
            OnboardingGrantState(accessibilityEnabled = false, isDefaultHome = false),
        )
        assertEquals(OnboardingStep.DEFAULT_HOME, pages[pages.size - 2])
        assertEquals(OnboardingStep.ACCESSIBILITY, pages.last())
        assertTrue(OnboardingLogic.isLastPage(pages.lastIndex, pages.size))
    }

    @Test
    fun pagesOmitGrantedPermissions() {
        val pages = OnboardingLogic.pages(
            OnboardingGrantState(accessibilityEnabled = true, isDefaultHome = true),
        )
        assertEquals(OnboardingLogic.introSteps, pages)
        assertFalse(pages.contains(OnboardingStep.ACCESSIBILITY))
        assertFalse(pages.contains(OnboardingStep.DEFAULT_HOME))
    }

    @Test
    fun pagesKeepOnlyMissingHomeRole() {
        val pages = OnboardingLogic.pages(
            OnboardingGrantState(accessibilityEnabled = true, isDefaultHome = false),
        )
        assertEquals(OnboardingStep.DEFAULT_HOME, pages.last())
        assertFalse(pages.contains(OnboardingStep.ACCESSIBILITY))
    }

    @Test
    fun accessibilityListedParsesColonSeparatedServices() {
        val component = "dev.electrikjesus.xrlauncher/dev.electrikjesus.xrlauncher.accessibility.DisplayPointerAccessibilityService"
        assertTrue(
            OnboardingLogic.isAccessibilityListed(
                "com.other/.Svc:$component",
                component,
            ),
        )
        assertFalse(
            OnboardingLogic.isAccessibilityListed(
                "com.other/.Svc",
                component,
            ),
        )
        assertFalse(OnboardingLogic.isAccessibilityListed(null, component))
    }
}
