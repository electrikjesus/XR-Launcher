package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R

object AllAppsLauncher {
    const val BOUNDS_KEY = "__all_apps_launcher__"
    /** Marker for companion hover state — not shown as user-facing label. */
    const val HOVER_LABEL = "__all_apps__"
}

@Composable
fun AllAppsLauncherCell(
    isHovered: Boolean,
    onBoundsChanged: (String, androidx.compose.ui.geometry.Rect) -> Unit,
    onOpenAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onOpenAllApps)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(AllAppsLauncher.BOUNDS_KEY, coordinates.boundsInRoot())
            }
            .background(
                if (isHovered) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            tint = if (isHovered) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(52.dp),
        )
        Text(
            text = stringResource(R.string.all_apps),
            style = MaterialTheme.typography.labelSmall,
            color = if (isHovered) Color(0xFF03DAC5) else Color.White.copy(alpha = 0.9f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
