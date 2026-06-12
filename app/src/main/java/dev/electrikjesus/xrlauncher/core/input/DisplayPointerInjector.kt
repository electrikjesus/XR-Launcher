package dev.electrikjesus.xrlauncher.core.input

import dev.electrikjesus.xrlauncher.accessibility.DisplayPointerAccessibilityService

/** Bridges companion clicks to the optional accessibility display-pointer service. */
object DisplayPointerInjector {
    @Volatile
    var service: DisplayPointerAccessibilityService? = null

    val isAvailable: Boolean get() = service != null

    fun dispatchClick(displayId: Int, normalizedX: Float, normalizedY: Float, button: PointerButton): Boolean {
        return service?.dispatchClick(displayId, normalizedX, normalizedY, button) ?: false
    }
}
