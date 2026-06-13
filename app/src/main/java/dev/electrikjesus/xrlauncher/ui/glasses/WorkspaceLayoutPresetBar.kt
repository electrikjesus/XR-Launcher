package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset

object WorkspaceLayoutPresetBar {
    fun boundsKey(preset: LayoutPreset): String = "__layout_preset_${preset.name}__"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceLayoutPresetBar(
    activePreset: LayoutPreset?,
    onPresetSelected: (LayoutPreset) -> Unit,
    onBoundsChanged: (String, Rect) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LayoutPreset.entries.forEach { preset ->
            FilterChip(
                selected = activePreset == preset,
                onClick = { onPresetSelected(preset) },
                label = { Text(presetLabel(preset)) },
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    onBoundsChanged(
                        WorkspaceLayoutPresetBar.boundsKey(preset),
                        coordinates.boundsInRoot(),
                    )
                },
            )
        }
    }
}

@Composable
private fun presetLabel(preset: LayoutPreset): String = when (preset) {
    LayoutPreset.STANDARD -> stringResource(R.string.layout_preset_standard)
    LayoutPreset.SINGLE -> stringResource(R.string.layout_preset_single)
    LayoutPreset.DUAL -> stringResource(R.string.layout_preset_dual)
    LayoutPreset.TRIPTYCH -> stringResource(R.string.layout_preset_triptych)
}
