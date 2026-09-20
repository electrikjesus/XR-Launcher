package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.display.HostGlassesAttachLogic

private val CardBg = Color(0xF21C1C1E)
private val ScrimBg = Color(0x99000000)
private val Accent = Color(0xFF8AB4F8)
private val ChipBg = Color(0xFF3A3A3C)

/** Ask what to put on the newly attached glasses / secondary display. */
@Composable
fun BoxScope.HostGlassesDisplayDialogLayer(
    onChoice: (HostGlassesAttachLogic.Choice) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(12f)
            .background(ScrimBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    )
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .zIndex(13f)
            .widthIn(min = 320.dp, max = 480.dp)
            .fillMaxWidth(0.42f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.host_glasses_attach_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.host_glasses_attach_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
        )
        ChoiceRow(
            title = stringResource(R.string.host_glasses_attach_xr_ui),
            hint = stringResource(R.string.host_glasses_attach_xr_ui_hint),
            onClick = { onChoice(HostGlassesAttachLogic.Choice.XR_GLASSES_UI) },
        )
        ChoiceRow(
            title = stringResource(R.string.host_glasses_attach_android_desktop),
            hint = stringResource(R.string.host_glasses_attach_android_desktop_hint),
            onClick = { onChoice(HostGlassesAttachLogic.Choice.ANDROID_DESKTOP) },
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Accent,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
