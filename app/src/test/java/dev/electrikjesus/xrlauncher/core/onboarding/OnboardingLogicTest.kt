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
    fun lastPageIsAccessibility() {
        assertEquals(OnboardingStep.ACCESSIBILITY, OnboardingLogic.steps.last())
        assertTrue(OnboardingLogic.isLastPage(OnboardingLogic.pageCount() - 1))
        assertFalse(OnboardingLogic.isLastPage(0))
    }
}
