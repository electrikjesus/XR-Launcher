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

private val GlassPanelShape = RoundedCornerShape(28.dp)
private val DockShape = RoundedCornerShape(36.dp)

@Composable
fun WorkspacePanelShell(
    panelId: String,
    isFocused: Boolean,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val focusColor = Color(0xFF9CDCFE)
    val borderColor = if (isFocused) focusColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.14f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPanelBoundsChanged(panelId, coordinates.boundsInRoot())
            },
        shape = GlassPanelShape,
        color = Color(0xCC141820),
        tonalElevation = if (isFocused) 10.dp else 3.dp,
        shadowElevation = if (isFocused) 16.dp else 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Column {
            when {
                header != null -> header()
                title != null -> {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isFocused) focusColor else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun WorkspaceDockShell(
    panelId: String,
    isFocused: Boolean,
    onPanelBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val focusColor = Color(0xFF9CDCFE)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPanelBoundsChanged(panelId, coordinates.boundsInRoot())
            },
        shape = DockShape,
        color = Color(0xB3181C24),
        tonalElevation = if (isFocused) 8.dp else 2.dp,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isFocused) focusColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            content()
        }
    }
}
