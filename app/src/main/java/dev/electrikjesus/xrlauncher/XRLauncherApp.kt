package dev.electrikjesus.xrlauncher

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
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.core.display.DisplayLaunchHelper
import dev.electrikjesus.xrlauncher.ui.desktop.SpatialDesktopScreen
import dev.electrikjesus.xrlauncher.ui.phone.PhoneShellScreen

@Composable
fun XRLauncherApp(
    capabilityDetector: CapabilityDetector,
    onRefreshCapabilities: () -> Unit,
) {
    val context = LocalContext.current
    val appRepository = remember { AppRepository(context) }
    val appLauncher = remember { AppLauncher(context) }
    val apps = remember { appRepository.loadLaunchableApps() }
    val capabilities by capabilityDetector.capabilities.collectAsState()

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
                onLaunchApp = { appLauncher.launchOnDefaultDisplay(it.componentName) },
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
                onLaunchApp = { appLauncher.launchOnDefaultDisplay(it.componentName) },
                onOpenGlassesWorkspace = {
                    val displayId = capabilities.secondaryDisplayIds.firstOrNull()
                        ?: DisplayLaunchHelper.findSecondaryDisplayId(context)
                    if (displayId != null) {
                        DisplayLaunchHelper.openGlassesSession(context, displayId)
                    }
                    onRefreshCapabilities()
                },
                onOpenCompanion = {
                    DisplayLaunchHelper.openCompanionController(context)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
