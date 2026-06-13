package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R

/** Phase 3.4 — chrome actions for an embedded or pseudo-embedded app panel. */
@Composable
fun PanelEmbedOrbiterBar(
    hostedLabel: String?,
    onFocus: () -> Unit,
    onPopOut: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hostedLabel.isNullOrBlank()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = hostedLabel,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onFocus) {
            Text(stringResource(R.string.panel_embed_focus))
        }
        OutlinedButton(onClick = onPopOut) {
            Text(stringResource(R.string.panel_embed_pop_out))
        }
        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.panel_embed_close))
        }
    }
}
