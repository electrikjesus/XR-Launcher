package dev.electrikjesus.xrlauncher.core.workspace

import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How companion pointer aims the Home Space camera. */
enum class GlassesLookMode {
    /** Cursor moves on the HUD; camera yaws/pitches a little from cursor offset. */
    GRADIENT,

    /** Cursor locked to view center; pointer deltas rotate the camera 1:1 (FPS). */
    FPS,
    ;

    companion object {
        private val _preference = MutableStateFlow(GRADIENT)
        val preferenceFlow: StateFlow<GlassesLookMode> = _preference.asStateFlow()

        var preference: GlassesLookMode
            get() = _preference.value
            set(value) {
                _preference.value = value
            }

        /**
         * FPS only while the launcher is in front. Launching an app unlocks the cursor
         * (gradient pointing); coming back to Home Space restores [preference].
         */
        fun effective(): GlassesLookMode =
            if (GlassesSessionState.launcherForeground) preference else GRADIENT

        fun fromPersisted(raw: String?): GlassesLookMode =
            if (raw.equals("fps", ignoreCase = true)) FPS else GRADIENT
    }
}
