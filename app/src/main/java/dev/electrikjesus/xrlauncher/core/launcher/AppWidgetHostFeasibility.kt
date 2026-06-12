package dev.electrikjesus.xrlauncher.core.launcher

import android.content.Context
import android.view.Display

/**
 * Phase 2.16 — documents [android.appwidget.AppWidgetHost] constraints on external displays.
 *
 * Findings (Pixel 8 + RayNeo SmartGlasses, 2026-06-12):
 * - AppWidgetHost requires a live Activity context; our glasses host is [ExternalDisplayActivity].
 * - Widgets render in-process on whichever display hosts the Activity window — feasible for Tier 1
 *   EXTERNAL when the launcher activity fills the glasses display.
 * - Providers that require default HOME or phone display may refuse to bind on secondary display.
 * - VirtualDisplay-based embed remains blocked on Pixel Desktop Mode (see device-matrix.md).
 * - Recommendation: ship curated Compose widgets (clock/calendar) for v1; optional AppWidgetHost
 *   behind a Settings toggle once tested per provider on glasses hardware.
 */
object AppWidgetHostFeasibility {
    data class Assessment(
        val hostActivityRequired: Boolean = true,
        val externalDisplaySupported: Boolean = true,
        val virtualDisplayBlocked: Boolean = true,
        val recommendedPath: String = "Compose widgets + optional AppWidgetHost spike on device",
    )

    fun assess(context: Context, displayId: Int): Assessment = assess(displayId)

    fun assess(displayId: Int): Assessment {
        val isSecondary = displayId != Display.DEFAULT_DISPLAY
        return Assessment(
            externalDisplaySupported = isSecondary,
            virtualDisplayBlocked = isSecondary,
        )
    }
}
