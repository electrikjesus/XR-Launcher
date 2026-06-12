package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPanelBoundsChanged(panelId, coordinates.boundsInRoot())
            }
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) focusColor else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (isFocused) focusColor else Color.White.copy(alpha = 0.55f),
            modifier = titleBarModifier.padding(bottom = 4.dp),
        )
        content()
    }
}
