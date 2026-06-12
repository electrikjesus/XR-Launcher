package dev.electrikjesus.xrlauncher.core.launcher

import android.content.ComponentName
import android.content.Context
import android.util.Log
import dev.electrikjesus.xrlauncher.core.capability.SpatialEmbedCapability
import dev.electrikjesus.xrlauncher.core.workspace.EmbedMode
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState

/**
 * Phase 3 — launch an app for a workspace panel slot.
 * Attempts spatial embed when the platform supports it; otherwise full-window on target display.
 */
class PanelAppLauncher(
    private val context: Context,
    private val appLauncher: AppLauncher,
    private val embedRegistry: PanelEmbedRegistry? = null,
) {
    fun launchInPanel(
        panel: PanelState,
        componentName: ComponentName,
        displayId: Int,
        moveLauncherToBack: () -> Unit = {},
    ): PanelLaunchResult {
        val embedMode = SpatialEmbedCapability.preferredEmbedMode(context, panel)
        Log.d(TAG, "launchInPanel panel=${panel.id} embedMode=$embedMode displayId=$displayId")
        return when (embedMode) {
            EmbedMode.EMBEDDED -> {
                val registry = embedRegistry
                if (registry != null && registry.canEmbed() &&
                    registry.embedLaunch(panel, componentName)
                ) {
                    PanelLaunchResult.Embedded
                } else {
                    Log.i(TAG, "Spatial embed unavailable — falling back to full window")
                    launchFullWindow(componentName, displayId, moveLauncherToBack)
                    PanelLaunchResult.FullWindowFallback(
                        reason = "Spatial embed unavailable on this device",
                    )
                }
            }
            EmbedMode.FULL_WINDOW -> {
                launchFullWindow(componentName, displayId, moveLauncherToBack)
                PanelLaunchResult.FullWindow
            }
            EmbedMode.NONE -> {
                launchFullWindow(componentName, displayId, moveLauncherToBack)
                PanelLaunchResult.FullWindow
            }
        }
    }

    private fun launchFullWindow(
        componentName: ComponentName,
        displayId: Int,
        moveLauncherToBack: () -> Unit,
    ) {
        appLauncher.launchOnDisplay(componentName, displayId)
        moveLauncherToBack()
    }

    companion object {
        private const val TAG = "XRLauncher/PanelLaunch"
    }
}

sealed class PanelLaunchResult {
    data object FullWindow : PanelLaunchResult()
    data class FullWindowFallback(val reason: String) : PanelLaunchResult()
    data object Embedded : PanelLaunchResult()
}

fun PanelState.isLaunchSlot(): Boolean =
    kind == PanelKind.EMPTY_SLOT || (kind == PanelKind.WIDGET && id == "empty_slot")
