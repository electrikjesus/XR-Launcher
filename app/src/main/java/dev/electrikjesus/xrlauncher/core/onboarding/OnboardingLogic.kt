package dev.electrikjesus.xrlauncher.core.onboarding

enum class OnboardingStep {
    WELCOME,
    PHONE_HOME,
    CONNECT_GLASSES,
    TOUCHPAD,
    DEFAULT_HOME,
    ACCESSIBILITY,
}

data class OnboardingGrantState(
    val accessibilityEnabled: Boolean,
    val isDefaultHome: Boolean,
) {
    /** Grants the wizard must keep nagging about until the user enables them. */
    fun allRequiredGranted(): Boolean = accessibilityEnabled && isDefaultHome

    fun hasMissingRequired(): Boolean = !allRequiredGranted()
}

object OnboardingLogic {
    val introSteps: List<OnboardingStep> = listOf(
        OnboardingStep.WELCOME,
        OnboardingStep.PHONE_HOME,
        OnboardingStep.CONNECT_GLASSES,
        OnboardingStep.TOUCHPAD,
    )

    /**
     * @param includeIntro First-run or Settings replay shows the welcome pages.
     * Permission-only rechecks (completed before, grants revoked) skip the intro.
     */
    fun pages(
        grants: OnboardingGrantState,
        includeIntro: Boolean = true,
    ): List<OnboardingStep> = buildList {
        if (includeIntro) addAll(introSteps)
        if (!grants.isDefaultHome) add(OnboardingStep.DEFAULT_HOME)
        if (!grants.accessibilityEnabled) add(OnboardingStep.ACCESSIBILITY)
        // Never return an empty pager — e.g. grants flip mid-session while still visible.
        if (isEmpty()) addAll(introSteps)
    }

    /**
     * Show on every launch when a required grant is missing, on first run (no glasses yet),
     * or when Settings requests a replay. Completing / skipping the wizard must not hide
     * missing Accessibility or Home role forever.
     */
    fun shouldShow(
        completed: Boolean,
        replayRequested: Boolean,
        hasSecondaryDisplay: Boolean,
        grants: OnboardingGrantState,
    ): Boolean {
        if (replayRequested) return true
        if (grants.hasMissingRequired()) return true
        return !completed && !hasSecondaryDisplay
    }

    fun includeIntroPages(
        completed: Boolean,
        replayRequested: Boolean,
    ): Boolean = replayRequested || !completed

    fun isLastPage(index: Int, pageCount: Int): Boolean =
        pageCount > 0 && index >= pageCount - 1

    fun isPermissionStep(step: OnboardingStep): Boolean =
        step == OnboardingStep.DEFAULT_HOME || step == OnboardingStep.ACCESSIBILITY

    fun isAccessibilityListed(enabledServices: String?, componentFlatten: String): Boolean {
        if (enabledServices.isNullOrBlank() || componentFlatten.isBlank()) return false
        return enabledServices.split(':', ';').any { token ->
            token.equals(componentFlatten, ignoreCase = true)
        }
    }
}
