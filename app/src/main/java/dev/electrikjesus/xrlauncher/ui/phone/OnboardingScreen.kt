package dev.electrikjesus.xrlauncher.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingLogic
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingStep
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingLogic.pageCount() })
    val scope = rememberCoroutineScope()
    val lastPage = OnboardingLogic.isLastPage(pagerState.currentPage)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onFinished) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            OnboardingPage(step = OnboardingLogic.steps[page])
        }
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingLogic.steps.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                            },
                        ),
                )
            }
        }
        if (lastPage) {
            Button(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_get_started))
            }
        } else {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_next))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingPage(step: OnboardingStep) {
    val icon = iconFor(step)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp),
            )
        }
        Text(
            text = stringResource(titleFor(step)),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            text = stringResource(bodyFor(step)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun iconFor(step: OnboardingStep): ImageVector = when (step) {
    OnboardingStep.WELCOME -> Icons.Default.ViewInAr
    OnboardingStep.PHONE_HOME -> Icons.Default.Home
    OnboardingStep.CONNECT_GLASSES -> Icons.Default.Visibility
    OnboardingStep.TOUCHPAD -> Icons.Default.TouchApp
    OnboardingStep.ACCESSIBILITY -> Icons.Default.AccessibilityNew
}

private fun titleFor(step: OnboardingStep): Int = when (step) {
    OnboardingStep.WELCOME -> R.string.onboarding_welcome_title
    OnboardingStep.PHONE_HOME -> R.string.onboarding_phone_title
    OnboardingStep.CONNECT_GLASSES -> R.string.onboarding_connect_title
    OnboardingStep.TOUCHPAD -> R.string.onboarding_touchpad_title
    OnboardingStep.ACCESSIBILITY -> R.string.onboarding_accessibility_title
}

private fun bodyFor(step: OnboardingStep): Int = when (step) {
    OnboardingStep.WELCOME -> R.string.onboarding_welcome_body
    OnboardingStep.PHONE_HOME -> R.string.onboarding_phone_body
    OnboardingStep.CONNECT_GLASSES -> R.string.onboarding_connect_body
    OnboardingStep.TOUCHPAD -> R.string.onboarding_touchpad_body
    OnboardingStep.ACCESSIBILITY -> R.string.onboarding_accessibility_body
}
