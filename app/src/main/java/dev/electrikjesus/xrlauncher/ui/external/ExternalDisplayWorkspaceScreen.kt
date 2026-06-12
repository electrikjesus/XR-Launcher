package dev.electrikjesus.xrlauncher.ui.external

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerButton
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.ui.glasses.GlassesSpatialWorkspaceScreen
import dev.electrikjesus.xrlauncher.ui.glasses.resolveHotseatApps

@Composable
fun ExternalDisplayWorkspaceScreen(
    apps: List<LaunchableApp>,
    onLaunchApp: (LaunchableApp) -> Unit,
    onRightClick: (LaunchableApp?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cursor by CompanionPointerBus.cursor.collectAsState()
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }
    var lastLaunchAtMs by remember { mutableLongStateOf(0L) }
    val hotseatApps = remember(apps) { resolveHotseatApps(apps) }
    val gridApps = remember(apps, hotseatApps) {
        apps.filter { app -> hotseatApps.none { it.componentName == app.componentName } }
    }

    fun cursorPoint(normalizedX: Float, normalizedY: Float): Offset =
        Offset(normalizedX * rootWidthPx, normalizedY * rootHeightPx)

    fun findAppAt(point: Offset): LaunchableApp? {
        val key = itemBounds.entries.firstOrNull { (_, rect) -> rect.contains(point) }?.key
        return key?.let { k -> apps.find { it.componentName.flattenToString() == k } }
    }

    LaunchedEffect(Unit) {
        CompanionPointerBus.clicks.collect { click ->
            val now = System.currentTimeMillis()
            if (now - lastLaunchAtMs < LAUNCH_DEBOUNCE_MS) return@collect
            val app = findAppAt(cursorPoint(click.x, click.y))
            Log.d(LOG_TAG, "click ${click.button} at (${click.x}, ${click.y}) hit=${app?.label}")
            when (click.button) {
                PointerButton.LEFT -> {
                    if (app != null) {
                        lastLaunchAtMs = now
                        onLaunchApp(app)
                    }
                }
                PointerButton.RIGHT -> onRightClick(app)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        rootWidthPx = with(density) { maxWidth.toPx() }
        rootHeightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(cursor.x, cursor.y, itemBounds.size, rootWidthPx, rootHeightPx) {
            CompanionPointerBus.setHoveredLabel(
                findAppAt(cursorPoint(cursor.x, cursor.y))?.label,
            )
        }

        GlassesSpatialWorkspaceScreen(
            apps = gridApps,
            hotseatApps = hotseatApps,
            onBoundsChanged = { key, rect -> itemBounds[key] = rect },
        )
    }
}

private const val LOG_TAG = "XRLauncher/Pointer"
private const val LAUNCH_DEBOUNCE_MS = 1_000L
