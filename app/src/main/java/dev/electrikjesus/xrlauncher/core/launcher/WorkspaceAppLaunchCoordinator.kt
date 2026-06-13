package dev.electrikjesus.xrlauncher.core.launcher

import android.app.Activity
import android.content.ComponentName
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
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
        val focusedPanelId = CompanionPointerBus.focusedPanelId.value
        val panel = when (focusedPanelId) {
            "empty_slot" -> PanelState(id = "empty_slot", kind = PanelKind.EMPTY_SLOT, visible = true)
            else -> PanelState(id = "full_window", kind = PanelKind.EMPTY_SLOT)
        }
        if (panel.id == "empty_slot") {
            assignEmptySlotHost(app)
        }
        return panelLauncher.launchInPanel(
            panel = panel,
            componentName = app.componentName,
            displayId = displayId,
            moveLauncherToBack = moveLauncherToBack,
        )
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
        appLauncher.launchOnDisplay(component, displayId)
        return true
    }

    fun closeEmbedded(panelId: String) {
        embedRegistry?.closeEmbedded(panelId)
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
