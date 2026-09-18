package dev.electrikjesus.xrlauncher.core.launcher

import android.app.Activity
import android.content.ComponentName
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.electrikjesus.xrlauncher.core.capability.SpatialEmbedCapability
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.workspace.GlassesAppPlane
import dev.electrikjesus.xrlauncher.core.workspace.GlassesHomeLook
import dev.electrikjesus.xrlauncher.core.workspace.PanelKind
import dev.electrikjesus.xrlauncher.core.workspace.PanelState
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.core.workspace.componentKey
import kotlinx.coroutines.launch

/** Unified panel-aware app launch for glasses and Tier 0 spatial desktop. */
class WorkspaceAppLaunchCoordinator(
    private val activity: Activity,
    private val appLauncher: AppLauncher,
    private val workspaceRepository: WorkspaceRepository,
    embedRegistry: PanelEmbedRegistry? = null,
) {
    private val panelLauncher = PanelAppLauncher(activity, appLauncher, embedRegistry)
    val embedRegistry: PanelEmbedRegistry? = embedRegistry

    fun launchFromGlasses(
        app: LaunchableApp,
        displayId: Int,
        moveLauncherToBack: () -> Unit,
    ): PanelLaunchResult {
        if (tryOpenInFocusedPlane(app)) {
            return PanelLaunchResult.Embedded
        }
        return panelLauncher.launchInPanel(
            panel = PanelState(id = "full_window", kind = PanelKind.EMPTY_SLOT),
            componentName = app.componentName,
            displayId = displayId,
            moveLauncherToBack = moveLauncherToBack,
        )
    }

    /**
     * Host [app] on a new Home Space plane when Jetpack XR activity embed is live.
     * Previous planes stay to the left of the new focus. Returns false to fullscreen.
     */
    private fun tryOpenInFocusedPlane(app: LaunchableApp): Boolean {
        val registry = embedRegistry ?: return false
        if (!SpatialEmbedCapability.canOpenInFocusedPlane(
                hasSpatialApi = SpatialEmbedCapability.hasSpatialApi(activity),
                canEmbedActivity = registry.canEmbed(),
            )
        ) {
            return false
        }
        val panelId = "app_${app.packageName}"
        val panel = PanelState(
            id = panelId,
            kind = PanelKind.EMPTY_SLOT,
            visible = true,
            hostedComponentKey = app.componentKey(),
        )
        if (!registry.embedLaunch(panel, app.componentName)) {
            return false
        }
        GlassesHomeLook.openAppPlane(
            GlassesAppPlane(
                panelId = panelId,
                componentKey = app.componentKey(),
                label = app.label,
            ),
        )
        CompanionPointerBus.setFocusedPanelId(panelId)
        return true
    }

    fun launchFromSpatialDesktop(
        app: LaunchableApp,
        visiblePanels: List<PanelState>,
        focusedPanelIndex: Int,
        displayId: Int = Display.DEFAULT_DISPLAY,
    ): PanelLaunchResult {
        val focusedPanel = visiblePanels.getOrNull(focusedPanelIndex)
        val panel = when {
            focusedPanel?.id == "empty_slot" -> focusedPanel.copy(visible = true)
            focusedPanel?.kind == PanelKind.EMPTY_SLOT -> focusedPanel.copy(visible = true)
            else -> PanelState(id = "full_window", kind = PanelKind.EMPTY_SLOT)
        }
        if (panel.id == "empty_slot") {
            assignEmptySlotHost(app)
        }
        return panelLauncher.launchInPanel(
            panel = panel,
            componentName = app.componentName,
            displayId = displayId,
        )
    }

    fun popOutEmbedded(panelId: String, displayId: Int = Display.DEFAULT_DISPLAY): Boolean {
        val registry = embedRegistry ?: return false
        val component = registry.getHostedComponent(panelId) ?: return false
        registry.closeEmbedded(panelId)
        GlassesHomeLook.closeAppPlane(panelId)
        appLauncher.launchOnDisplay(component, displayId)
        return true
    }

    fun closeEmbedded(panelId: String) {
        embedRegistry?.closeEmbedded(panelId)
        GlassesHomeLook.closeAppPlane(panelId)
        if (panelId == "empty_slot") {
            (activity as? ComponentActivity)?.lifecycleScope?.launch {
                workspaceRepository.assignPanelHost("empty_slot", componentKey = null)
            }
        }
    }

    private fun assignEmptySlotHost(app: LaunchableApp) {
        (activity as? ComponentActivity)?.lifecycleScope?.launch {
            workspaceRepository.assignPanelHost("empty_slot", app.componentKey())
            workspaceRepository.setPanelVisible("empty_slot", visible = true)
        }
    }

    companion object {
        fun componentFromKey(key: String): ComponentName? =
            parseComponentKey(key)?.let { (pkg, cls) -> ComponentName(pkg, cls) }

        /** Pure parse for unit tests and [componentFromKey]. */
        internal fun parseComponentKey(key: String): Pair<String, String>? {
            val slash = key.indexOf('/')
            if (slash <= 0) return null
            val pkg = key.substring(0, slash)
            val cls = key.substring(slash + 1).let { if (it.startsWith(".")) pkg + it else it }
            if (pkg.isBlank() || cls.isBlank()) return null
            return pkg to cls
        }
    }
}
