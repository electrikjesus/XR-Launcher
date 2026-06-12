package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.workspace.LayoutPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceLayoutPresetBar(
    activePreset: LayoutPreset?,
    onPresetSelected: (LayoutPreset) -> Unit,
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
