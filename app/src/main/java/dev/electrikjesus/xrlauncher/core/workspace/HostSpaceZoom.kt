package dev.electrikjesus.xrlauncher.core.workspace

/**
 * Maps host scroll-wheel and pinch gestures onto [HomeSpaceTuneAxis.SPHERE] deltas.
 *
 * Larger [WorkspaceAppearance.sphereScale] pushes panes farther (smaller on screen);
 * scroll-up / pinch-out decrease sphere scale (zoom in).
 */
object HostSpaceZoom {
    /** Compose scrollDelta pixels roughly equal to one mouse-wheel notch. */
    const val SCROLL_PIXELS_PER_STEP = 64f

    /** How strongly pinch distance ratio maps to sphere scale. */
    const val PINCH_GAIN = 0.45f

    fun sphereDeltaFromScroll(scrollY: Float): Float {
        if (scrollY == 0f) return 0f
        return (scrollY / SCROLL_PIXELS_PER_STEP) * HomeSpaceTune.STEP
    }

    fun sphereDeltaFromPinch(previousDistance: Float, currentDistance: Float): Float {
        if (previousDistance < 1f || currentDistance < 1f) return 0f
        val ratio = currentDistance / previousDistance
        // Fingers apart (ratio > 1) → zoom in → decrease sphere scale.
        return (1f - ratio) * PINCH_GAIN
    }
}
