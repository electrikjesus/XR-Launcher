package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Scales dp and text consistently across the launcher shell. */
@Composable
fun WorkspaceScaledLayer(
    uiScale: Float,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * uiScale,
            fontScale = density.fontScale * uiScale,
        ),
    ) {
        content()
    }
}
