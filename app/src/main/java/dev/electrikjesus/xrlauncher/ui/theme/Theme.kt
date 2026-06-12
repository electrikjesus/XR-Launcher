package dev.electrikjesus.xrlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
)

private val GlassesColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    background = Color.Black,
    surface = Color(0xFF121212),
)

@Composable
fun XRLauncherTheme(
    forGlasses: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (forGlasses) GlassesColors else LightColors,
        content = content,
    )
}
