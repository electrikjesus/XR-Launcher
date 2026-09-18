package dev.electrikjesus.xrlauncher.core.onboarding

enum class OnboardingStep {
    WELCOME,
    PHONE_HOME,
    CONNECT_GLASSES,
    TOUCHPAD,
    ACCESSIBILITY,
}

object OnboardingLogic {
    val steps: List<OnboardingStep> = OnboardingStep.entries

    fun shouldShow(
        completed: Boolean,
        replayRequested: Boolean,
        hasSecondaryDisplay: Boolean,
    ): Boolean {
        if (replayRequested) return true
        return !completed && !hasSecondaryDisplay
    }

    fun isLastPage(index: Int): Boolean = index >= steps.lastIndex

    fun pageCount(): Int = steps.size
}
