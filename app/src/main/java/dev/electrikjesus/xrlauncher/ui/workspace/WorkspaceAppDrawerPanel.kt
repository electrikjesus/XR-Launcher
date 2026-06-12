package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsGridConfig
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsPaginationState
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@Composable
fun WorkspaceAppDrawerPanel(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    apps: List<LaunchableApp>,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    modifier: Modifier = Modifier,
    useDarkTheme: Boolean = true,
    useSharedPagination: Boolean = false,
) {
    val textColor = if (useDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val accentColor = Color(0xFF03DAC5)
    var localPageIndex by remember { mutableIntStateOf(0) }
    val sharedPageIndex by AllAppsPaginationState.pageIndexFlow.collectAsState(initial = 0)
    val pageIndex = if (useSharedPagination) sharedPageIndex else localPageIndex

    LaunchedEffect(searchQuery) {
        if (useSharedPagination) {
            AllAppsPaginationState.goToPage(0)
        } else {
            localPageIndex = 0
        }
    }

    LaunchedEffect(apps.size, useSharedPagination) {
        if (useSharedPagination) {
            AllAppsPaginationState.updatePageCount(apps.size, AllAppsGridConfig.pageSize)
        }
    }

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
        PaginatedAppGrid(
            apps = apps,
            pageIndex = pageIndex,
            onPageChange = { index ->
                if (useSharedPagination) {
                    AllAppsPaginationState.goToPage(index)
                } else {
                    localPageIndex = index
                }
            },
            hoveredLabel = hoveredLabel,
            pinnedComponentKeys = pinnedComponentKeys,
            onBoundsChanged = onBoundsChanged,
            onLaunchApp = onLaunchApp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            useDarkTheme = useDarkTheme,
        )
    }
}
