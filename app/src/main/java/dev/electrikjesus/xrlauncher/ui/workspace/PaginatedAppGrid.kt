package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.AllAppsGridConfig
import dev.electrikjesus.xrlauncher.core.launcher.AppDrawerPagination
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp
import dev.electrikjesus.xrlauncher.core.workspace.componentKey

object AllAppsPageControls {
    const val PREV_KEY = "__all_apps_page_prev__"
    const val NEXT_KEY = "__all_apps_page_next__"
    const val PREV_HOVER = "Previous page"
    const val NEXT_HOVER = "Next page"

    fun pageKey(index: Int): String = "__all_apps_page_${index}__"

    fun pageHover(index: Int): String = "Page ${index + 1}"
}

@Composable
fun PaginatedAppGrid(
    apps: List<LaunchableApp>,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    hoveredLabel: String?,
    pinnedComponentKeys: Set<String>,
    onBoundsChanged: (String, Rect) -> Unit,
    onLaunchApp: ((LaunchableApp) -> Unit)?,
    onAppContextMenu: ((LaunchableApp, Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = AllAppsGridConfig.columns,
    rows: Int = AllAppsGridConfig.rows,
    useDarkTheme: Boolean = true,
    showPageControls: Boolean = true,
    iconSize: Dp = 52.dp,
    cellSpacing: Dp = 4.dp,
) {
    val textColor = if (useDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = textColor.copy(alpha = 0.65f)
    val accentColor = Color(0xFF03DAC5)
    val pageSize = columns * rows
    val pageCount = AppDrawerPagination.pageCount(apps.size, pageSize)
    val safePageIndex = AppDrawerPagination.clampPageIndex(pageIndex, pageCount)
    val pageApps = AppDrawerPagination.pageApps(apps, safePageIndex, pageSize)

    LaunchedEffect(apps.size, pageSize) {
        if (safePageIndex != pageIndex) {
            onPageChange(safePageIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showPageControls && pageCount > 1) {
                PageNavButton(
                    enabled = safePageIndex > 0,
                    boundsKey = AllAppsPageControls.PREV_KEY,
                    hovered = hoveredLabel == AllAppsPageControls.PREV_HOVER,
                    onBoundsChanged = onBoundsChanged,
                    onClick = { onPageChange(safePageIndex - 1) },
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.all_apps_page_prev),
                        tint = if (safePageIndex > 0) accentColor else mutedColor,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(cellSpacing),
            ) {
                if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_apps_found),
                            color = mutedColor,
                        )
                    }
                } else {
                    val cells = pageApps.withFillers(pageSize)
                    for (rowIndex in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                        ) {
                            for (colIndex in 0 until columns) {
                                val cellIndex = rowIndex * columns + colIndex
                                val app = cells.getOrNull(cellIndex)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (app != null) {
                                        AppIconCell(
                                            app = app,
                                            isHovered = hoveredLabel == app.label,
                                            isPinned = app.componentKey() in pinnedComponentKeys,
                                            onBoundsChanged = onBoundsChanged,
                                            onLaunchApp = onLaunchApp,
                                            onContextMenu = onAppContextMenu,
                                            iconSize = iconSize,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showPageControls && pageCount > 1) {
                PageNavButton(
                    enabled = safePageIndex < pageCount - 1,
                    boundsKey = AllAppsPageControls.NEXT_KEY,
                    hovered = hoveredLabel == AllAppsPageControls.NEXT_HOVER,
                    onBoundsChanged = onBoundsChanged,
                    onClick = { onPageChange(safePageIndex + 1) },
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.all_apps_page_next),
                        tint = if (safePageIndex < pageCount - 1) accentColor else mutedColor,
                    )
                }
            }
        }

        if (showPageControls && pageCount > 1) {
            AllAppsPageButtonRow(
                currentPage = safePageIndex,
                pageCount = pageCount,
                hoveredLabel = hoveredLabel,
                onPageChange = onPageChange,
                onBoundsChanged = onBoundsChanged,
                accentColor = accentColor,
                mutedColor = mutedColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.all_apps_page_status,
                    safePageIndex + 1,
                    pageCount,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PageNavButton(
    enabled: Boolean,
    boundsKey: String,
    hovered: Boolean,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 64.dp, minHeight = 64.dp)
            .background(
                if (hovered) Color(0xFF8AB4F8).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(12.dp),
            )
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(boundsKey, coordinates.boundsInRoot())
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun AllAppsPageButtonRow(
    currentPage: Int,
    pageCount: Int,
    hoveredLabel: String?,
    onPageChange: (Int) -> Unit,
    onBoundsChanged: (String, Rect) -> Unit,
    accentColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier,
) {
    val visiblePages = AppDrawerPagination.visiblePageButtons(currentPage, pageCount)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visiblePages.forEach { page ->
            val selected = page == currentPage
            OutlinedButton(
                onClick = { onPageChange(page) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(
                        if (hoveredLabel == AllAppsPageControls.pageHover(page)) {
                            Color(0xFF8AB4F8).copy(alpha = 0.35f)
                        } else {
                            Color.Transparent
                        },
                        RoundedCornerShape(10.dp),
                    )
                    .onGloballyPositioned { coordinates ->
                        onBoundsChanged(AllAppsPageControls.pageKey(page), coordinates.boundsInRoot())
                    },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = (page + 1).toString(),
                    color = if (selected) accentColor else mutedColor,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

private fun List<LaunchableApp>.withFillers(pageSize: Int): List<LaunchableApp?> {
    if (isEmpty()) return emptyList()
    val fillers = pageSize - size
    return this + List(fillers.coerceAtLeast(0)) { null }
}
