package dev.electrikjesus.xrlauncher.core.input

import dev.electrikjesus.xrlauncher.accessibility.DisplayPointerAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bridges companion pointer state to the accessibility display-pointer service. */
object DisplayPointerInjector {
    private val _isAvailable = MutableStateFlow(false)
    val isAvailableFlow: StateFlow<Boolean> = _isAvailable.asStateFlow()

    @Volatile
    var service: DisplayPointerAccessibilityService? = null
        set(value) {
            field = value
            _isAvailable.value = value != null
        }

    val isAvailable: Boolean get() = service != null

    fun dispatchClick(
        displayId: Int,
        normalizedX: Float,
        normalizedY: Float,
        button: PointerButton,
        mapViaLauncherFrame: Boolean = false,
    ): Boolean {
        return service?.dispatchClick(
            displayId,
            normalizedX,
            normalizedY,
            button,
            mapViaLauncherFrame,
        ) ?: false
    }

    fun dispatchDrag(
        displayId: Int,
        fromNormalizedX: Float,
        fromNormalizedY: Float,
        toNormalizedX: Float,
        toNormalizedY: Float,
        mapViaLauncherFrame: Boolean = false,
    ): Boolean {
        return service?.dispatchDrag(
            displayId,
            fromNormalizedX,
            fromNormalizedY,
            toNormalizedX,
            toNormalizedY,
            mapViaLauncherFrame,
        ) ?: false
    }

    /** Cursor is over a PIP / foreign app window that Compose cannot hit. */
    fun shouldInjectOverForeignWindow(normalizedX: Float, normalizedY: Float): Boolean =
        service?.shouldInjectOverForeignWindow(normalizedX, normalizedY) == true
}
