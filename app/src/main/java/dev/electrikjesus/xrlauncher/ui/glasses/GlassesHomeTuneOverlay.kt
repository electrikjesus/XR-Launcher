package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.GlassesHomeHits
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTune
import dev.electrikjesus.xrlauncher.core.workspace.HomeSpaceTuneAxis
import dev.electrikjesus.xrlauncher.core.workspace.WorkspaceAppearance
import dev.electrikjesus.xrlauncher.ui.workspace.WorkspaceScaledLayer
import java.util.Locale

private val PillBg = Color(0xCC1C1C1E)
private val CardBg = Color(0xF21C1C1E)
private val Accent = Color(0xFF8AB4F8)

@Composable
fun GlassesHomeTuneOverlay(
    appearance: WorkspaceAppearance,
    hoveredLabel: String?,
    editing: Boolean,
    onBoundsChanged: (String, Rect) -> Unit,
    onToggleEdit: () -> Unit,
    onNudge: (HomeSpaceTuneAxis, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuned = appearance.clamped()
    WorkspaceScaledLayer(uiScale = tuned.uiScale) {
        Column(
            modifier = modifier.padding(32.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (editing) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 420.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(CardBg)
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = stringResource(R.string.xr_edit_space_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                    )
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
                }
            }
            Box(
                modifier = Modifier
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
