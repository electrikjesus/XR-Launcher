package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.workspace.GlassesLookMode
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceEditPage
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTune
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import java.util.Locale

private val CardBg = Color(0xF21C1C1E)
private val ScrimBg = Color(0x99000000)
private val Accent = Color(0xFF8AB4F8)
private val ChipBg = Color(0xFF3A3A3C)

/**
 * Host Edit as a screen-space modal above the desk pointer catcher (same layer as the
 * radial menu). Unscaled — [WorkspaceAppearance.uiScale] is for desk icons, not this card.
 */
@Composable
fun BoxScope.HostEditDialogLayer(
    appearance: WorkspaceAppearance,
    onClose: () -> Unit,
    onNudge: (HomeSpaceTuneAxis, Float) -> Unit,
) {
    val tuned = appearance.clamped()
    val editPage by GlassesSessionState.homeSpaceEditPageFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9f)
            .background(ScrimBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 320.dp, max = 420.dp)
                .fillMaxWidth(0.42f)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    style = MaterialTheme.typography.titleLarge,
                )
                HostEditIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.all_apps_close),
                    onClick = onClose,
                )
            }
            HostEditPageTabs(page = editPage)
            when (editPage) {
                HomeSpaceEditPage.PERSPECTIVE -> {
                    HostTuneRow(
                        label = stringResource(R.string.xr_edit_panel_scale),
                        value = tuned.panelScale,
                        onMinus = { onNudge(HomeSpaceTuneAxis.PANEL, -HomeSpaceTune.STEP) },
                        onPlus = { onNudge(HomeSpaceTuneAxis.PANEL, HomeSpaceTune.STEP) },
                    )
                    HostTuneRow(
                        label = stringResource(R.string.xr_edit_sphere_scale),
                        value = tuned.sphereScale,
                        onMinus = { onNudge(HomeSpaceTuneAxis.SPHERE, -HomeSpaceTune.STEP) },
                        onPlus = { onNudge(HomeSpaceTuneAxis.SPHERE, HomeSpaceTune.STEP) },
                    )
                    HostTuneRow(
                        label = stringResource(R.string.xr_edit_element_scale),
                        value = tuned.uiScale,
                        onMinus = { onNudge(HomeSpaceTuneAxis.ELEMENT, -HomeSpaceTune.STEP) },
                        onPlus = { onNudge(HomeSpaceTuneAxis.ELEMENT, HomeSpaceTune.STEP) },
                    )
                    HostDeskToggleRow(
                        label = stringResource(R.string.xr_edit_look_fps),
                        enabled = tuned.lookMode == GlassesLookMode.FPS,
                        onToggle = { onNudge(HomeSpaceTuneAxis.LOOK_FPS, 0f) },
                    )
                }
                HomeSpaceEditPage.DESKTOP -> {
                    HostDeskToggleRow(
                        label = stringResource(R.string.xr_edit_desk_icons),
                        enabled = tuned.desktopIcons,
                        onToggle = { onNudge(HomeSpaceTuneAxis.DESK_ICONS, 0f) },
                    )
                    HostDeskToggleRow(
                        label = stringResource(R.string.xr_edit_desk_piles),
                        enabled = tuned.desktopPiles,
                        onToggle = { onNudge(HomeSpaceTuneAxis.DESK_PILES, 0f) },
                    )
                    HostDeskToggleRow(
                        label = stringResource(R.string.xr_edit_desk_tiles),
                        enabled = tuned.desktopTiles,
                        onToggle = { onNudge(HomeSpaceTuneAxis.DESK_TILES, 0f) },
                    )
                    HostDeskToggleRow(
                        label = stringResource(R.string.xr_edit_desk_widgets),
                        enabled = tuned.desktopWidgets,
                        onToggle = { onNudge(HomeSpaceTuneAxis.DESK_WIDGETS, 0f) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HostEditPageTabs(page: HomeSpaceEditPage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HostEditTab(
            label = stringResource(R.string.xr_edit_page_perspective),
            selected = page == HomeSpaceEditPage.PERSPECTIVE,
            onClick = { GlassesSessionState.homeSpaceEditPage = HomeSpaceEditPage.PERSPECTIVE },
            modifier = Modifier.weight(1f),
        )
        HostEditTab(
            label = stringResource(R.string.xr_edit_page_desktop),
            selected = page == HomeSpaceEditPage.DESKTOP,
            onClick = { GlassesSessionState.homeSpaceEditPage = HomeSpaceEditPage.DESKTOP },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HostEditTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Accent.copy(alpha = 0.28f) else ChipBg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun HostDeskToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) Accent.copy(alpha = 0.28f) else ChipBg)
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(if (enabled) R.string.xr_edit_on else R.string.xr_edit_off),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun HostTuneRow(
    label: String,
    value: Float,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        HostEditIconButton(icon = Icons.Default.Remove, contentDescription = label, onClick = onMinus)
        Text(
            text = String.format(Locale.US, "%.2f", value),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        HostEditIconButton(icon = Icons.Default.Add, contentDescription = label, onClick = onPlus)
    }
}

@Composable
private fun HostEditIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ChipBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}
