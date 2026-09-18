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
)

object OnboardingLogic {
    val introSteps: List<OnboardingStep> = listOf(
        OnboardingStep.WELCOME,
        OnboardingStep.PHONE_HOME,
        OnboardingStep.CONNECT_GLASSES,
        OnboardingStep.TOUCHPAD,
    )

    fun pages(grants: OnboardingGrantState): List<OnboardingStep> = buildList {
        addAll(introSteps)
        if (!grants.isDefaultHome) add(OnboardingStep.DEFAULT_HOME)
        if (!grants.accessibilityEnabled) add(OnboardingStep.ACCESSIBILITY)
    }

    fun shouldShow(
        completed: Boolean,
        replayRequested: Boolean,
        hasSecondaryDisplay: Boolean,
    ): Boolean {
        if (replayRequested) return true
        return !completed && !hasSecondaryDisplay
    }

    fun isLastPage(index: Int, pageCount: Int = pages(OnboardingGrantState(false, false)).size): Boolean =
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
