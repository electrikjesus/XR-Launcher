package dev.electrikjesus.xrlauncher.core.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingLogicTest {
    private val missingGrants = OnboardingGrantState(
        accessibilityEnabled = false,
        isDefaultHome = false,
        notificationListenerEnabled = false,
    )
    private val fullGrants = OnboardingGrantState(
        accessibilityEnabled = true,
        isDefaultHome = true,
        notificationListenerEnabled = true,
    )

    @Test
    fun showsOnFirstLaunchWithoutDisplay() {
        assertTrue(
            OnboardingLogic.shouldShow(
                completed = false,
                replayRequested = false,
                hasSecondaryDisplay = false,
                grants = fullGrants,
            ),
        )
    }

    @Test
    fun hidesWhenCompletedAndGrantsOk() {
        assertFalse(
            OnboardingLogic.shouldShow(
                completed = true,
                replayRequested = false,
                hasSecondaryDisplay = false,
                grants = fullGrants,
            ),
        )
    }

    @Test
    fun skipsIntroWhenGlassesAlreadyConnectedAndGrantsOk() {
        assertFalse(
            OnboardingLogic.shouldShow(
                completed = false,
                replayRequested = false,
                hasSecondaryDisplay = true,
                grants = fullGrants,
            ),
        )
    }

    @Test
    fun showsWhenRequiredGrantsMissingEvenIfCompleted() {
        assertTrue(
            OnboardingLogic.shouldShow(
                completed = true,
                replayRequested = false,
                hasSecondaryDisplay = true,
                grants = missingGrants,
            ),
        )
    }

    @Test
    fun showsWhenOnlyAccessibilityMissing() {
        assertTrue(
            OnboardingLogic.shouldShow(
                completed = true,
                replayRequested = false,
                hasSecondaryDisplay = false,
                grants = OnboardingGrantState(
                    accessibilityEnabled = false,
                    isDefaultHome = true,
                    notificationListenerEnabled = true,
                ),
            ),
        )
    }

    @Test
    fun showsWhenOnlyNotificationListenerMissing() {
        assertTrue(
            OnboardingLogic.shouldShow(
                completed = true,
                replayRequested = false,
                hasSecondaryDisplay = true,
                grants = OnboardingGrantState(
                    accessibilityEnabled = true,
                    isDefaultHome = true,
                    notificationListenerEnabled = false,
                ),
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
                grants = fullGrants,
            ),
        )
    }

    @Test
    fun pagesIncludePermissionsWhenMissing() {
        val pages = OnboardingLogic.pages(missingGrants)
        assertEquals(OnboardingStep.DEFAULT_HOME, pages[pages.size - 3])
        assertEquals(OnboardingStep.ACCESSIBILITY, pages[pages.size - 2])
        assertEquals(OnboardingStep.NOTIFICATIONS, pages.last())
        assertTrue(OnboardingLogic.isLastPage(pages.lastIndex, pages.size))
    }

    @Test
    fun pagesOmitGrantedPermissions() {
        val pages = OnboardingLogic.pages(fullGrants)
        assertEquals(OnboardingLogic.introSteps, pages)
        assertFalse(pages.contains(OnboardingStep.ACCESSIBILITY))
        assertFalse(pages.contains(OnboardingStep.DEFAULT_HOME))
        assertFalse(pages.contains(OnboardingStep.NOTIFICATIONS))
    }

    @Test
    fun pagesKeepOnlyMissingHomeRole() {
        val pages = OnboardingLogic.pages(
            OnboardingGrantState(
                accessibilityEnabled = true,
                isDefaultHome = false,
                notificationListenerEnabled = true,
            ),
        )
        assertEquals(OnboardingStep.DEFAULT_HOME, pages.last())
        assertFalse(pages.contains(OnboardingStep.ACCESSIBILITY))
        assertFalse(pages.contains(OnboardingStep.NOTIFICATIONS))
    }

    @Test
    fun permissionRecheck_skipsIntroPages() {
        val pages = OnboardingLogic.pages(missingGrants, includeIntro = false)
        assertEquals(
            listOf(
                OnboardingStep.DEFAULT_HOME,
                OnboardingStep.ACCESSIBILITY,
                OnboardingStep.NOTIFICATIONS,
            ),
            pages,
        )
        assertFalse(OnboardingLogic.includeIntroPages(completed = true, replayRequested = false))
        assertTrue(OnboardingLogic.includeIntroPages(completed = false, replayRequested = false))
        assertTrue(OnboardingLogic.includeIntroPages(completed = true, replayRequested = true))
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
