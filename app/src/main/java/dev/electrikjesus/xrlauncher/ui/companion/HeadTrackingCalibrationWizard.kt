package dev.electrikjesus.xrlauncher.ui.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.rayneo.HeadTrackingCalibrationSession
import dev.electrikjesus.xrlauncher.core.input.rayneo.HeadTrackingCalibrationStep
import dev.electrikjesus.xrlauncher.core.input.rayneo.HeadTrackingCalibrationStore
import dev.electrikjesus.xrlauncher.core.input.rayneo.RayNeoHeadTrackingState

@Composable
fun HeadTrackingCalibrationWizard(
    headTrackingState: RayNeoHeadTrackingState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val calibration by HeadTrackingCalibrationStore.calibration.collectAsState()
    val session by HeadTrackingCalibrationSession.uiState.collectAsState()
    val streaming = headTrackingState == RayNeoHeadTrackingState.STREAMING

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.xr_head_tracking_calibration_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = when {
                session.active -> stringResource(R.string.xr_head_tracking_calibration_active_hint)
                calibration.isCalibrated ->
                    stringResource(R.string.xr_head_tracking_calibration_done_hint)
                else -> stringResource(R.string.xr_head_tracking_calibration_intro_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        CalibrationCheckRow(
            label = stringResource(R.string.xr_head_tracking_calibration_pitch),
            complete = session.active && session.pitchComplete,
        )
        CalibrationCheckRow(
            label = stringResource(R.string.xr_head_tracking_calibration_yaw),
            complete = session.active && session.yawComplete,
        )
        CalibrationCheckRow(
            label = stringResource(R.string.xr_head_tracking_calibration_roll),
            complete = session.active && session.rollComplete,
        )

        if (session.active && session.currentStep != HeadTrackingCalibrationStep.COMPLETE) {
            Text(
                text = stepPrompt(session.currentStep),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
            LinearProgressIndicator(
                progress = { session.motionProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.xr_head_tracking_calibration_motion_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (session.active) {
                OutlinedButton(
                    onClick = { HeadTrackingCalibrationSession.cancel() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                OutlinedButton(
                    onClick = {
                        CompanionPointerBus.recenterHeadLook()
                        HeadTrackingCalibrationSession.start(context)
                    },
                    enabled = streaming,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        if (calibration.isCalibrated) {
                            stringResource(R.string.xr_head_tracking_calibration_recalibrate)
                        } else {
                            stringResource(R.string.xr_head_tracking_calibration_start)
                        },
                    )
                }
                if (calibration.isCalibrated) {
                    OutlinedButton(
                        onClick = {
                            HeadTrackingCalibrationStore.clear(context)
                            CompanionPointerBus.recenterHeadLook()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(stringResource(R.string.xr_head_tracking_calibration_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationCheckRow(
    label: String,
    complete: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (complete) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Checkbox(
                checked = false,
                onCheckedChange = null,
                enabled = false,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (complete) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun stepPrompt(step: HeadTrackingCalibrationStep): String = when (step) {
    HeadTrackingCalibrationStep.LOOK_UP ->
        stringResource(R.string.xr_head_tracking_calibration_step_look_up)
    HeadTrackingCalibrationStep.LOOK_DOWN ->
        stringResource(R.string.xr_head_tracking_calibration_step_look_down)
    HeadTrackingCalibrationStep.LOOK_LEFT ->
        stringResource(R.string.xr_head_tracking_calibration_step_look_left)
    HeadTrackingCalibrationStep.LOOK_RIGHT ->
        stringResource(R.string.xr_head_tracking_calibration_step_look_right)
    HeadTrackingCalibrationStep.TILT_LEFT ->
        stringResource(R.string.xr_head_tracking_calibration_step_tilt_left)
    HeadTrackingCalibrationStep.TILT_RIGHT ->
        stringResource(R.string.xr_head_tracking_calibration_step_tilt_right)
    HeadTrackingCalibrationStep.COMPLETE ->
        stringResource(R.string.xr_head_tracking_calibration_step_complete)
}
