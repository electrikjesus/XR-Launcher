package dev.electrikjesus.xrlauncher

import android.content.Intent
import android.hardware.display.DisplayManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import dev.electrikjesus.xrlauncher.companion.CompanionControllerActivity
import dev.electrikjesus.xrlauncher.core.capability.CapabilityDetector
import dev.electrikjesus.xrlauncher.core.capability.LayoutFormFactor
import dev.electrikjesus.xrlauncher.core.capability.RuntimeTier
import dev.electrikjesus.xrlauncher.core.launcher.AppLauncher
import dev.electrikjesus.xrlauncher.core.launcher.AppRepository
import dev.electrikjesus.xrlauncher.glasses.GlassesLauncherActivity
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
                        ?: appLauncher.findSecondaryDisplayId()
                    if (displayId != null) {
                        val options = android.app.ActivityOptions.makeBasic()
                            .setLaunchDisplayId(displayId)
                            .toBundle()
                        context.startActivity(
                            Intent(context, GlassesLauncherActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                            options,
                        )
                    } else {
                        context.startActivity(
                            Intent(context, GlassesLauncherActivity::class.java),
                        )
                    }
                    onRefreshCapabilities()
                },
                onOpenCompanion = {
                    context.startActivity(Intent(context, CompanionControllerActivity::class.java))
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
