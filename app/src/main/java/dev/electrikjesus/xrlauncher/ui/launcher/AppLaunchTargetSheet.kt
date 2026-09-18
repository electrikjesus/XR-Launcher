package dev.electrikjesus.xrlauncher.ui.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.launcher.AppLaunchChoice
import dev.electrikjesus.xrlauncher.core.launcher.AppLaunchChooser
import dev.electrikjesus.xrlauncher.core.launcher.AppLaunchTarget
import dev.electrikjesus.xrlauncher.core.launcher.LaunchableApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLaunchTargetSheet(
    app: LaunchableApp,
    hasSecondaryDisplay: Boolean,
    onSelect: (AppLaunchTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.launch_target_title, app.label),
                style = MaterialTheme.typography.titleLarge,
            )
            if (!hasSecondaryDisplay) {
                Text(
                    text = stringResource(R.string.launch_target_xr_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.launch_target_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
            }
            AppLaunchChooser.choices(hasSecondaryDisplay).forEach { choice ->
                LaunchTargetRow(
                    choice = choice,
                    onClick = {
                        onSelect(choice.target)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun LaunchTargetRow(
    choice: AppLaunchChoice,
    onClick: () -> Unit,
) {
    val enabled = choice.enabled
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = choice.target.icon(),
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else contentColor,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(choice.target.titleRes()),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
            Text(
                text = stringResource(choice.target.hintRes()),
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    contentColor
                },
            )
        }
    }
}

private fun AppLaunchTarget.icon(): ImageVector = when (this) {
    AppLaunchTarget.PHONE -> Icons.Default.PhoneAndroid
    AppLaunchTarget.XR_EMBEDDED -> Icons.Default.ViewInAr
    AppLaunchTarget.XR_FULLSCREEN -> Icons.Default.Fullscreen
}

private fun AppLaunchTarget.titleRes(): Int = when (this) {
    AppLaunchTarget.PHONE -> R.string.launch_target_phone
    AppLaunchTarget.XR_EMBEDDED -> R.string.launch_target_xr_embedded
    AppLaunchTarget.XR_FULLSCREEN -> R.string.launch_target_xr_fullscreen
}

private fun AppLaunchTarget.hintRes(): Int = when (this) {
    AppLaunchTarget.PHONE -> R.string.launch_target_phone_hint
    AppLaunchTarget.XR_EMBEDDED -> R.string.launch_target_xr_embedded_hint
    AppLaunchTarget.XR_FULLSCREEN -> R.string.launch_target_xr_fullscreen_hint
}
