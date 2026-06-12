package dev.electrikjesus.xrlauncher.core.input

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.core.display.GlassesControlMode

/** Visual sizing for companion / in-activity cursors (overlay uses [DisplayCursorOverlayManager]). */
data class CursorVisualStyle(
    val dotSize: Dp,
    val dotAlpha: Float,
) {
    val halfDotSize: Dp get() = dotSize / 2
}

object CursorStyles {
    /** Small semi-transparent pointer over third-party apps (Desktop mode). */
    val desktop = CursorVisualStyle(dotSize = 14.dp, dotAlpha = 0.55f)

    /** Larger pointer for launcher hit-testing on glasses (Launcher mode). */
    val launcher = CursorVisualStyle(dotSize = 26.dp, dotAlpha = 0.85f)

    fun forControlMode(mode: GlassesControlMode): CursorVisualStyle =
        when (mode) {
            GlassesControlMode.DESKTOP -> desktop
            GlassesControlMode.LAUNCHER -> launcher
        }
}
