package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R

object GlassesWorkspaceTitleBar {
    const val BOUNDS_KEY = "__launcher_settings__"
    const val HOVER_LABEL = "Settings"
}

@Composable
fun GlassesWorkspaceTitleBar(
    onOpenSettings: () -> Unit,
    onBoundsChanged: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
    /** When true, only a small settings affordance is shown (glasses display). */
    compact: Boolean = true,
) {
    if (compact) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .onGloballyPositioned { coordinates ->
                        onBoundsChanged(
                            GlassesWorkspaceTitleBar.BOUNDS_KEY,
                            coordinates.boundsInRoot(),
                        )
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.settings_open),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onBoundsChanged(
                    GlassesWorkspaceTitleBar.BOUNDS_KEY,
                    coordinates.boundsInRoot(),
                )
            },
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.settings_open),
            )
        }
    }
}
