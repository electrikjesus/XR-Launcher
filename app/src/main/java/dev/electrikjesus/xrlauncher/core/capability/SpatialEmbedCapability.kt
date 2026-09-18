package dev.electrikjesus.xrlauncher.core.capability

import android.content.Context
import dev.electrikjesus.xrlauncher.core.workspace.EmbedMode
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState

/** Phase 3.2 — spatial activity embedding capability checks. */
object SpatialEmbedCapability {
    fun hasSpatialApi(context: Context): Boolean =
        context.packageManager.hasSystemFeature(CapabilityDetector.FEATURE_XR_API_SPATIAL)

    fun canEmbedActivities(context: Context): Boolean = hasSpatialApi(context)

    /** True when a third-party app can occupy a focused Home Space plane. */
    fun canOpenInFocusedPlane(hasSpatialApi: Boolean, canEmbedActivity: Boolean): Boolean =
        hasSpatialApi && canEmbedActivity

    fun preferredEmbedMode(context: Context, panel: PanelState): EmbedMode =
        preferredEmbedMode(hasSpatialApi(context), panel)

    fun preferredEmbedMode(hasSpatialApi: Boolean, panel: PanelState): EmbedMode {
        if (!hasSpatialApi) return EmbedMode.FULL_WINDOW
        return when (panel.kind) {
            PanelKind.EMPTY_SLOT -> EmbedMode.EMBEDDED
            PanelKind.WIDGET -> EmbedMode.FULL_WINDOW
            PanelKind.APP_DRAWER, PanelKind.HOTSEAT -> EmbedMode.NONE
        }
    }

    fun launchLabel(context: Context, panel: PanelState): String =
        launchLabel(hasSpatialApi(context), panel)

    fun launchLabel(hasSpatialApi: Boolean, panel: PanelState): String =
        if (preferredEmbedMode(hasSpatialApi, panel) == EmbedMode.EMBEDDED) {
            "Spatial window"
        } else {
            "Full launch"
        }
}
