package dev.electrikjesus.xrlauncher.ui.companion

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/** Haptic cues for companion pointer clicks — touchpad gestures and on-screen buttons. */
object CompanionPointerHaptics {
    fun leftClick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun rightClick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
    }

    fun pressDown(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }
}
