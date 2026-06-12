package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.AppDrawerItem
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.componentKey

@Composable
fun WorkspaceAppDrawerPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    drawerItems: List<AppDrawerItem>,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    modifier: Modifier = Modifier,
    useDarkTheme: Boolean = true,
) {
    val textColor = if (useDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = textColor.copy(alpha = 0.6f)
    val accentColor = Color(0xFF03DAC5)

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.7f),
                )
            },
            placeholder = {
                Text(
                    stringResource(R.string.search_apps),
                    color = textColor.copy(alpha = 0.5f),
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor.copy(alpha = 0.9f),
                cursorColor = accentColor,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = textColor.copy(alpha = 0.35f),
            ),
        )
        if (drawerItems.isEmpty()) {
            Text(
                text = stringResource(R.string.no_apps_found),
                color = mutedColor,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(
                    items = drawerItems,
                    key = { item ->
                        when (item) {
                            is AppDrawerItem.SectionHeader -> "header-${item.letter}"
                            is AppDrawerItem.AppEntry -> item.app.componentName.flattenToString()
                        }
                    },
                    span = { item ->
                        when (item) {
                            is AppDrawerItem.SectionHeader -> GridItemSpan(maxLineSpan)
                            is AppDrawerItem.AppEntry -> GridItemSpan(1)
                        }
                    },
                ) { item ->
                    when (item) {
                        is AppDrawerItem.SectionHeader -> {
                            Text(
                                text = item.letter.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        is AppDrawerItem.AppEntry -> {
                            AppIconCell(
                                app = item.app,
                                isHovered = hoveredLabel == item.app.label,
                                isPinned = item.app.componentKey() in pinnedComponentKeys,
                                onBoundsChanged = onBoundsChanged,
                                onLaunchApp = onLaunchApp,
                            )
                        }
                    }
                }
            }
        }
    }
}
