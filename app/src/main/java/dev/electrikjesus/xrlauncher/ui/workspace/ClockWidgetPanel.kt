package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun ClockWidgetPanel(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date(), key1 = Unit) {
        while (true) {
            value = Date()
            delay(30_000)
        }
    }
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(now)
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(now)

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
