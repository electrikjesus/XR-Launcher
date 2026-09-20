package dev.electrikjesus.xrlauncher.ui.phone

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingGrantState
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingLogic
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingPermissions
import dev.electrikjesus.xrlauncher.core.onboarding.OnboardingStep
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    includeIntro: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var grants by remember { mutableStateOf(OnboardingPermissions.snapshot(context)) }
    val pages = remember(grants, includeIntro) { OnboardingLogic.pages(grants, includeIntro) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    val homeRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        grants = OnboardingPermissions.snapshot(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                grants = OnboardingPermissions.snapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pages.size) {
        if (pagerState.currentPage >= pages.size && pages.isNotEmpty()) {
            pagerState.scrollToPage(pages.lastIndex)
        }
    }

    val currentStep = pages.getOrElse(pagerState.currentPage) { OnboardingStep.WELCOME }
    val lastPage = OnboardingLogic.isLastPage(pagerState.currentPage, pages.size)
    val permissionGranted = permissionGranted(currentStep, grants)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
            val step = pages.getOrElse(page) { OnboardingStep.WELCOME }
            OnboardingPage(
                step = step,
                granted = permissionGranted(step, grants),
            )
        }
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pages.indices.forEach { index ->
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
        if (OnboardingLogic.isPermissionStep(currentStep) && !permissionGranted) {
            if (currentStep == OnboardingStep.ACCESSIBILITY) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(OnboardingPermissions.appInfoIntent(context))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.onboarding_open_app_info))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    when (currentStep) {
                        OnboardingStep.DEFAULT_HOME ->
                            homeRoleLauncher.launch(OnboardingPermissions.requestHomeIntent(context))
                        OnboardingStep.ACCESSIBILITY ->
                            context.startActivity(OnboardingPermissions.accessibilitySettingsIntent())
                        OnboardingStep.NOTIFICATIONS ->
                            context.startActivity(OnboardingPermissions.notificationListenerSettingsIntent())
                        else -> Unit
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(actionLabelFor(currentStep)))
            }
            FilledTonalButton(
                onClick = {
                    if (lastPage) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = if (lastPage) {
                        stringResource(R.string.onboarding_get_started)
                    } else {
                        stringResource(R.string.onboarding_later)
                    },
                )
            }
        } else if (lastPage) {
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
private fun OnboardingPage(
    step: OnboardingStep,
    granted: Boolean,
) {
    val icon = if (granted && OnboardingLogic.isPermissionStep(step)) {
        Icons.Default.CheckCircle
    } else {
        iconFor(step)
    }
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
        if (OnboardingLogic.isPermissionStep(step)) {
            Text(
                text = stringResource(
                    if (granted) R.string.onboarding_permission_granted else R.string.onboarding_permission_needed,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private fun permissionGranted(step: OnboardingStep, grants: OnboardingGrantState): Boolean =
    when (step) {
        OnboardingStep.DEFAULT_HOME -> grants.isDefaultHome
        OnboardingStep.ACCESSIBILITY -> grants.accessibilityEnabled
        OnboardingStep.NOTIFICATIONS -> grants.notificationListenerEnabled
        else -> false
    }

private fun iconFor(step: OnboardingStep): ImageVector = when (step) {
    OnboardingStep.WELCOME -> Icons.Default.ViewInAr
    OnboardingStep.PHONE_HOME -> Icons.Default.Home
    OnboardingStep.CONNECT_GLASSES -> Icons.Default.Visibility
    OnboardingStep.TOUCHPAD -> Icons.Default.TouchApp
    OnboardingStep.DEFAULT_HOME -> Icons.Default.Home
    OnboardingStep.ACCESSIBILITY -> Icons.Default.AccessibilityNew
    OnboardingStep.NOTIFICATIONS -> Icons.Default.Notifications
}

private fun titleFor(step: OnboardingStep): Int = when (step) {
    OnboardingStep.WELCOME -> R.string.onboarding_welcome_title
    OnboardingStep.PHONE_HOME -> R.string.onboarding_phone_title
    OnboardingStep.CONNECT_GLASSES -> R.string.onboarding_connect_title
    OnboardingStep.TOUCHPAD -> R.string.onboarding_touchpad_title
    OnboardingStep.DEFAULT_HOME -> R.string.onboarding_home_title
    OnboardingStep.ACCESSIBILITY -> R.string.onboarding_accessibility_title
    OnboardingStep.NOTIFICATIONS -> R.string.onboarding_notifications_title
}

private fun bodyFor(step: OnboardingStep): Int = when (step) {
    OnboardingStep.WELCOME -> R.string.onboarding_welcome_body
    OnboardingStep.PHONE_HOME -> R.string.onboarding_phone_body
    OnboardingStep.CONNECT_GLASSES -> R.string.onboarding_connect_body
    OnboardingStep.TOUCHPAD -> R.string.onboarding_touchpad_body
    OnboardingStep.DEFAULT_HOME -> R.string.onboarding_home_body
    OnboardingStep.ACCESSIBILITY -> R.string.onboarding_accessibility_body
    OnboardingStep.NOTIFICATIONS -> R.string.onboarding_notifications_body
}

private fun actionLabelFor(step: OnboardingStep): Int = when (step) {
    OnboardingStep.DEFAULT_HOME -> R.string.onboarding_set_default_home
    OnboardingStep.ACCESSIBILITY -> R.string.onboarding_enable_accessibility
    OnboardingStep.NOTIFICATIONS -> R.string.onboarding_enable_notifications
    else -> R.string.onboarding_next
}
