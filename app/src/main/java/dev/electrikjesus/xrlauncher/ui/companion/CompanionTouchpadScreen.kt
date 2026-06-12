package dev.electrikjesus.xrlauncher.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import dev.electrikjesus.xrlauncher.core.input.PointerAction
import dev.electrikjesus.xrlauncher.core.input.PointerEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionTouchpadScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.companion_touchpad)) })
        },
    ) { padding ->
        Box(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            CompanionPointerBus.emit(PointerEvent(action = PointerAction.DOWN))
                        },
                        onDragEnd = {
                            CompanionPointerBus.emit(PointerEvent(action = PointerAction.UP))
                        },
                        onDragCancel = {
                            CompanionPointerBus.emit(PointerEvent(action = PointerAction.UP))
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        CompanionPointerBus.emit(
                            PointerEvent(
                                action = PointerAction.MOVE,
                                deltaX = dragAmount.x,
                                deltaY = dragAmount.y,
                            ),
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        CompanionPointerBus.emit(PointerEvent(action = PointerAction.DOWN))
                        CompanionPointerBus.emit(PointerEvent(action = PointerAction.UP))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.companion_hint),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
