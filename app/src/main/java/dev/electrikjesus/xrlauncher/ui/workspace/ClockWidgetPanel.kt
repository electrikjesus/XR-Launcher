package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
        modifier = modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
