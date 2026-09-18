package dev.electrikjesus.xrlauncher

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import dev.electrikjesus.xrlauncher.core.capability.CapabilityDetector
import dev.electrikjesus.xrlauncher.core.capability.LayoutFormFactor
import dev.electrikjesus.xrlauncher.core.capability.RuntimeTier
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.AppLaunchTarget
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.launcher.PanelEmbedRegistry
import dev.electrikjesus.xrlauncher.core.launcher.PhoneHomeLayout
import dev.electrikjesus.xrlauncher.core.launcher.WorkspaceAppLaunchCoordinator
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceRepository
import dev.electrikjesus.xrlauncher.settings.SettingsActivity
import dev.electrikjesus.xrlauncher.ui.desktop.SpatialDesktopScreen
import dev.electrikjesus.xrlauncher.ui.phone.PhoneShellScreen

@Composable
fun XRLauncherApp(
    capabilityDetector: CapabilityDetector,
    onRefreshCapabilities: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val appRepository = remember { AppRepository(context) }
    val appLauncher = remember { AppLauncher(context) }
    val workspaceRepository = remember { WorkspaceRepository(context.applicationContext) }
    val launchCoordinator = remember(activity) {
        val embedRegistry = PanelEmbedRegistry.fromActivity(activity)?.also {
            GlassesSessionState.panelEmbedRegistry = it
        }
        WorkspaceAppLaunchCoordinator(
            activity = activity,
            appLauncher = appLauncher,
            workspaceRepository = workspaceRepository,
            embedRegistry = embedRegistry,
        )
    }
    val apps = remember { appRepository.loadLaunchableApps() }
    val capabilities by capabilityDetector.capabilities.collectAsState()
    val workspace by workspaceRepository.workspace.collectAsState(initial = null)
    val hotseatApps = remember(apps, workspace?.hotseatPins) {
        PhoneHomeLayout.resolveHotseat(apps, workspace?.hotseatPins ?: emptyList())
    }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
    )
    val effectiveTier = if (isExpanded && capabilities.tier == RuntimeTier.PHONE_SHELL) {
        RuntimeTier.SPATIAL_DESKTOP
    } else {
        capabilities.tier
    }

    when (effectiveTier) {
        RuntimeTier.SPATIAL_DESKTOP,
        RuntimeTier.FULL_SPATIAL,
        -> {
            SpatialDesktopScreen(
                apps = apps,
                workspaceRepository = workspaceRepository,
                launchCoordinator = launchCoordinator,
                modifier = Modifier.fillMaxSize(),
            )
        }

        RuntimeTier.PHONE_SHELL,
        RuntimeTier.EXTERNAL_DISPLAY,
        RuntimeTier.PROJECTED_GLASSES,
        -> {
            PhoneShellScreen(
                apps = apps,
                capabilities = capabilities.copy(
                    formFactor = if (isExpanded) LayoutFormFactor.EXPANDED else LayoutFormFactor.COMPACT,
                ),
                hotseatApps = hotseatApps,
                onLaunchApp = { app, target ->
                    DisplayLaunchHelper.launchApp(context, app, target)
                },
                onOpenGlassesWorkspace = {
                    onRefreshCapabilities()
                    DisplayLaunchHelper.openGlassesSession(
                        context = context,
                        preferredDisplayId = capabilities.secondaryDisplayIds.firstOrNull(),
                    )
                },
                onOpenCompanion = {
                    DisplayLaunchHelper.openCompanionController(context)
                },
                onOpenSettings = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
