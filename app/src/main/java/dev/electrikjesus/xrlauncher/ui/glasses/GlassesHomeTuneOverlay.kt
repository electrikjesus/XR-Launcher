package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceDialogState
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceEditPage
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTune
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.ui.workspace.PanelTextureCapture
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceScaledLayer
import java.util.Locale

private val PillBg = Color(0xCC1C1C1E)
private val CardBg = Color(0xF21C1C1E)
private val Accent = Color(0xFF8AB4F8)

@Composable
fun BoxScope.GlassesHomeTuneOverlay(
    appearance: WorkspaceAppearance,
    hoveredLabel: String?,
    editing: Boolean,
    onBoundsChanged: (String, Rect) -> Unit,
    onToggleEdit: () -> Unit,
    onNudge: (HomeSpaceTuneAxis, Float) -> Unit,
    /** Capture Edit into a GLES view-locked dialog texture (hit-test still uses Compose bounds). */
    immersiveDialog: Boolean = false,
) {
    val tuned = appearance.clamped()
    val editPage by GlassesSessionState.homeSpaceEditPageFlow.collectAsState()
    val hostModal = GlassesSessionState.hostImmersiveSession
    // Host Edit/Settings live as siblings above the desk catcher in HostHomeSpaceScreen.
    val showEditCard = editing && !hostModal
    WorkspaceScaledLayer(uiScale = tuned.uiScale) {
        if (showEditCard) {
            val cardModifier = with(this@GlassesHomeTuneOverlay) {
                Modifier
                    .align(Alignment.Center)
                    .zIndex(4f)
            }
                .widthIn(min = 520.dp, max = 720.dp)
            val cardContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(36.dp))
                        .background(CardBg)
                        .padding(horizontal = 36.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                if (editPage == HomeSpaceEditPage.DESKTOP) {
                                    R.string.xr_edit_space_desktop_title
                                } else {
                                    R.string.xr_edit_space_title
                                },
                            ),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        TuneStepButton(
                            icon = Icons.Default.Close,
                            boundsKey = GlassesHomeHits.EDIT_CLOSE,
                            hovered = hoveredLabel == GlassesHomeHits.CLOSE_LABEL,
                            contentDescription = stringResource(R.string.all_apps_close),
                            onBoundsChanged = onBoundsChanged,
                            onClick = onToggleEdit,
                        )
                    }
                    EditPageTabs(
                        page = editPage,
                        hoveredLabel = hoveredLabel,
                        onBoundsChanged = onBoundsChanged,
                    )
                    when (editPage) {
                        HomeSpaceEditPage.PERSPECTIVE -> {
                            TuneRow(
                                label = stringResource(R.string.xr_edit_panel_scale),
                                value = tuned.panelScale,
                                minusKey = GlassesHomeHits.EDIT_PANEL_MINUS,
                                plusKey = GlassesHomeHits.EDIT_PANEL_PLUS,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onMinus = { onNudge(HomeSpaceTuneAxis.PANEL, -HomeSpaceTune.STEP) },
                                onPlus = { onNudge(HomeSpaceTuneAxis.PANEL, HomeSpaceTune.STEP) },
                            )
                            TuneRow(
                                label = stringResource(R.string.xr_edit_sphere_scale),
                                value = tuned.sphereScale,
                                minusKey = GlassesHomeHits.EDIT_SPHERE_MINUS,
                                plusKey = GlassesHomeHits.EDIT_SPHERE_PLUS,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onMinus = { onNudge(HomeSpaceTuneAxis.SPHERE, -HomeSpaceTune.STEP) },
                                onPlus = { onNudge(HomeSpaceTuneAxis.SPHERE, HomeSpaceTune.STEP) },
                            )
                            TuneRow(
                                label = stringResource(R.string.xr_edit_element_scale),
                                value = tuned.uiScale,
                                minusKey = GlassesHomeHits.EDIT_ELEMENT_MINUS,
                                plusKey = GlassesHomeHits.EDIT_ELEMENT_PLUS,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onMinus = { onNudge(HomeSpaceTuneAxis.ELEMENT, -HomeSpaceTune.STEP) },
                                onPlus = { onNudge(HomeSpaceTuneAxis.ELEMENT, HomeSpaceTune.STEP) },
                            )
                            DeskToggleRow(
                                label = stringResource(R.string.xr_edit_look_fps),
                                enabled = tuned.lookMode == GlassesLookMode.FPS,
                                boundsKey = GlassesHomeHits.EDIT_LOOK_FPS,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onToggle = { onNudge(HomeSpaceTuneAxis.LOOK_FPS, 0f) },
                            )
                        }
                        HomeSpaceEditPage.DESKTOP -> {
                            DeskToggleRow(
                                label = stringResource(R.string.xr_edit_desk_icons),
                                enabled = tuned.desktopIcons,
                                boundsKey = GlassesHomeHits.EDIT_DESK_ICONS,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onToggle = { onNudge(HomeSpaceTuneAxis.DESK_ICONS, 0f) },
                            )
                            DeskToggleRow(
                                label = stringResource(R.string.xr_edit_desk_piles),
                                enabled = tuned.desktopPiles,
                                boundsKey = GlassesHomeHits.EDIT_DESK_PILES,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onToggle = { onNudge(HomeSpaceTuneAxis.DESK_PILES, 0f) },
                            )
                            DeskToggleRow(
                                label = stringResource(R.string.xr_edit_desk_tiles),
                                enabled = tuned.desktopTiles,
                                boundsKey = GlassesHomeHits.EDIT_DESK_TILES,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onToggle = { onNudge(HomeSpaceTuneAxis.DESK_TILES, 0f) },
                            )
                            DeskToggleRow(
                                label = stringResource(R.string.xr_edit_desk_widgets),
                                enabled = tuned.desktopWidgets,
                                boundsKey = GlassesHomeHits.EDIT_DESK_WIDGETS,
                                hoveredLabel = hoveredLabel,
                                onBoundsChanged = onBoundsChanged,
                                onToggle = { onNudge(HomeSpaceTuneAxis.DESK_WIDGETS, 0f) },
                            )
                        }
                    }
                }
            }
            if (immersiveDialog) {
                PanelTextureCapture(
                    panelId = HomeSpaceDialogState.EDIT_TEXTURE_ID,
                    centerXNorm = 0.5f,
                    centerYNorm = 0.5f,
                    enabled = true,
                    drawToScreen = false,
                    modifier = cardModifier,
                    content = cardContent,
                )
            } else {
                Box(modifier = cardModifier, content = { cardContent() })
            }
        }
        EditTogglePill(
            editing = editing,
            hoveredLabel = hoveredLabel,
            onBoundsChanged = onBoundsChanged,
            onToggleEdit = onToggleEdit,
        )
    }
}

@Composable
private fun BoxScope.EditTogglePill(
    editing: Boolean,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onToggleEdit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .zIndex(4f)
            .padding(32.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                if (hoveredLabel == GlassesHomeHits.EDIT_LABEL) {
                    Accent.copy(alpha = 0.55f)
                } else {
                    PillBg
                },
            )
            .clickable(onClick = onToggleEdit)
            .onGloballyPositioned {
                onBoundsChanged(GlassesHomeHits.EDIT_TOGGLE, it.boundsInRoot())
            }
            .padding(horizontal = 40.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                if (editing) R.string.xr_edit_space_done else R.string.xr_edit_space,
            ),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun EditPageTabs(
    page: HomeSpaceEditPage,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditPageTab(
            label = stringResource(R.string.xr_edit_page_perspective),
            boundsKey = GlassesHomeHits.EDIT_PAGE_PERSPECTIVE,
            selected = page == HomeSpaceEditPage.PERSPECTIVE,
            hovered = hoveredLabel == GlassesHomeHits.EDIT_PAGE_PERSPECTIVE_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = { GlassesSessionState.homeSpaceEditPage = HomeSpaceEditPage.PERSPECTIVE },
            modifier = Modifier.weight(1f),
        )
        EditPageTab(
            label = stringResource(R.string.xr_edit_page_desktop),
            boundsKey = GlassesHomeHits.EDIT_PAGE_DESKTOP,
            selected = page == HomeSpaceEditPage.DESKTOP,
            hovered = hoveredLabel == GlassesHomeHits.EDIT_PAGE_DESKTOP_LABEL,
            onBoundsChanged = onBoundsChanged,
            onClick = { GlassesSessionState.homeSpaceEditPage = HomeSpaceEditPage.DESKTOP },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EditPageTab(
    label: String,
    boundsKey: String,
    selected: Boolean,
    hovered: Boolean,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                when {
                    hovered -> Accent.copy(alpha = 0.55f)
                    selected -> Accent.copy(alpha = 0.28f)
                    else -> Color(0xFF3A3A3C)
                },
            )
            .clickable(onClick = onClick)
            .onGloballyPositioned { onBoundsChanged(boundsKey, it.boundsInRoot()) }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DeskToggleRow(
    label: String,
    enabled: Boolean,
    boundsKey: String,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (hoveredLabel == GlassesHomeHits.hoverLabel(boundsKey)) {
                        Accent.copy(alpha = 0.55f)
                    } else if (enabled) {
                        Accent.copy(alpha = 0.28f)
                    } else {
                        Color(0xFF3A3A3C)
                    },
                )
                .clickable(onClick = onToggle)
                .onGloballyPositioned { onBoundsChanged(boundsKey, it.boundsInRoot()) }
                .padding(horizontal = 28.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(if (enabled) R.string.xr_edit_on else R.string.xr_edit_off),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TuneRow(
    label: String,
    value: Float,
    minusKey: String,
    plusKey: String,
    hoveredLabel: String?,
    onBoundsChanged: (String, Rect) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        TuneStepButton(
            icon = Icons.Default.Remove,
            boundsKey = minusKey,
            hovered = hoveredLabel == GlassesHomeHits.hoverLabel(minusKey),
            contentDescription = GlassesHomeHits.hoverLabel(minusKey).orEmpty(),
            onBoundsChanged = onBoundsChanged,
            onClick = onMinus,
        )
        Text(
            text = String.format(Locale.US, "%.2f", value),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        TuneStepButton(
            icon = Icons.Default.Add,
            boundsKey = plusKey,
            hovered = hoveredLabel == GlassesHomeHits.hoverLabel(plusKey),
            contentDescription = GlassesHomeHits.hoverLabel(plusKey).orEmpty(),
            onBoundsChanged = onBoundsChanged,
            onClick = onPlus,
        )
    }
}

@Composable
private fun TuneStepButton(
    icon: ImageVector,
    boundsKey: String,
    hovered: Boolean,
    contentDescription: String,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(if (hovered) Accent.copy(alpha = 0.55f) else Color(0xFF3A3A3C))
            .clickable(onClick = onClick)
            .onGloballyPositioned { onBoundsChanged(boundsKey, it.boundsInRoot()) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(44.dp))
    }
}
