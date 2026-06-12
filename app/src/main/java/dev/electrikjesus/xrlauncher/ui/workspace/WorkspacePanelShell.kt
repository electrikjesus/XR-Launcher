package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

private val GlassPanelShape = RoundedCornerShape(24.dp)

@Composable
fun WorkspacePanelShell(
    panelId: String,
    title: String,
    isFocused: Boolean,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
    titleBarModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val focusColor = Color(0xFF03DAC5)
    val borderColor = if (isFocused) focusColor else Color.White.copy(alpha = 0.18f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPanelBoundsChanged(panelId, coordinates.boundsInRoot())
            },
        shape = GlassPanelShape,
        color = Color(0xFF1A1520).copy(alpha = 0.82f),
        tonalElevation = if (isFocused) 8.dp else 4.dp,
        shadowElevation = if (isFocused) 12.dp else 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isFocused) focusColor else Color.White.copy(alpha = 0.65f),
                modifier = titleBarModifier.padding(bottom = 6.dp),
            )
            content()
        }
    }
}
