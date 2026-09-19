package dev.electrikjesus.xrlauncher.core.input

import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState

/**
 * How the Expanded host Home Space maps mouse / touch into desk + look.
 *
 * - [COMPANION_BUS] — legacy [dev.electrikjesus.xrlauncher.ui.host.HostPointerBridge]
 *   path that funnels absolute host pointer through companion FPS center-lock semantics.
 * - [BUMPDESK] — BumpDesk-derived absolute pointer (screen coords, touch slop, middle-drag
 *   look, no FPS press/release re-lock). Default for [GlassesSessionState.hostImmersiveSession].
 */
enum class HostInputMethod {
    COMPANION_BUS,
    BUMPDESK,
    ;

    companion object {
        /** Runtime switch for host Home Space; glasses / companion ignore this. */
        @Volatile
        var preference: HostInputMethod = BUMPDESK

        /** True when host immersive should skip companion FPS center-lock. */
        fun usesAbsoluteHostCursor(): Boolean =
            GlassesSessionState.hostImmersiveSession && preference == BUMPDESK
    }
}
