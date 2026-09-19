package dev.electrikjesus.xrlauncher.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.electrikjesus.xrlauncher.ui.phone.OnboardingScreen

private val CardBg = Color(0xF21C1C1E)
private val ScrimBg = Color(0x99000000)

/** Host onboarding / missing-permissions guide above the pointer catcher. */
@Composable
fun BoxScope.HostOnboardingDialogLayer(
    includeIntro: Boolean,
    onFinished: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(4f)
            .background(ScrimBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            ),
    )
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .zIndex(5f)
            .widthIn(min = 360.dp, max = 520.dp)
            .heightIn(max = 720.dp)
            .fillMaxWidth(0.46f)
            .fillMaxHeight(0.82f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(4.dp),
    ) {
        OnboardingScreen(
            onFinished = onFinished,
            includeIntro = includeIntro,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
