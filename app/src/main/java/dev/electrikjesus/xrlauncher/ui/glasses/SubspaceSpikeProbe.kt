package dev.electrikjesus.xrlauncher.ui.glasses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.electrikjesus.xrlauncher.core.display.GlassesSessionState
import dev.electrikjesus.xrlauncher.core.display.SubspaceSpike

/**
 * Wraps a Subspace tree and logs whether Compose reaches each stage on EXTERNAL display.
 * Debug banner stays on the 2D layer so we can tell Subspace rendered vs flat-only.
 */
@Composable
fun SubspaceSpikeProbe(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val decision = GlassesSessionState.subspaceDecision
    val displayId = GlassesSessionState.secondaryDisplayId

    SideEffect {
        SubspaceSpike.logCompositionStage("outer_2d_shell", displayId, decision)
    }

    LaunchedEffect(displayId, decision) {
        SubspaceSpike.logCompositionStage("outer_2d_launched", displayId, decision)
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        SideEffect {
            GlassesSessionState.subspaceOuterComposed = true
            SubspaceSpike.logCompositionStage("subspace_wrapper_entered", displayId, decision)
        }

        if (decision.forcedForSpike) {
            Text(
                text = "Subspace spike · api=${decision.hasSpatialApi} · forced=true",
                color = Color(0xFFFFAB40),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Call inside [androidx.xr.compose.spatial.Subspace] content to confirm inner pipeline ran. */
@Composable
fun SubspaceInnerSpikeMarker(stage: String) {
    val decision = GlassesSessionState.subspaceDecision
    val displayId = GlassesSessionState.secondaryDisplayId

    SideEffect {
        GlassesSessionState.subspaceInnerComposed = true
        SubspaceSpike.logCompositionStage(stage, displayId, decision, detail="inner_ok")
    }
}
