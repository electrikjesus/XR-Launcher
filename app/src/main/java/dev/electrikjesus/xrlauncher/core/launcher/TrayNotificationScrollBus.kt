package dev.electrikjesus.xrlauncher.core.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Companion / mouse-look scrubbing for the tray notification list.
 * Fraction 0 = top, 1 = bottom of the scrollable content.
 */
object TrayNotificationScrollBus {
    private val _scrubFraction = MutableStateFlow<Float?>(null)
    val scrubFractionFlow: StateFlow<Float?> = _scrubFraction.asStateFlow()

    fun scrubTo(fraction: Float) {
        _scrubFraction.value = fraction.coerceIn(0f, 1f)
    }

    fun clearScrub() {
        _scrubFraction.value = null
    }
}
