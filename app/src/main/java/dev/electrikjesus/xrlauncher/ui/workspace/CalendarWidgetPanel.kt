package dev.electrikjesus.xrlauncher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.electrikjesus.xrlauncher.R
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CalendarWidgetPanel(modifier: Modifier = Modifier) {
    val calendar by produceState(initialValue = Calendar.getInstance(), key1 = Unit) {
        while (true) {
            value = Calendar.getInstance()
            delay(60_000)
        }
    }
    val monthLabel = DateFormatSymbols.getInstance().months[calendar.get(Calendar.MONTH)]
    val year = calendar.get(Calendar.YEAR)
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val weekDays = DateFormatSymbols.getInstance().shortWeekdays
        .drop(1)
        .take(7)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.getFirstDayOfWeek()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    var startOffset = calendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek
    if (startOffset < 0) startOffset += 7

    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_widget_title, monthLabel, year),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            weekDays.forEach { label ->
                Text(
                    text = label.take(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        var day = 1
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val showDay = cellIndex >= startOffset && day <= daysInMonth
                    val cellDay = if (showDay) day++ else null
                    val isToday = cellDay == today
                    Text(
                        text = cellDay?.toString() ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isToday -> Color(0xFF03DAC5)
                            showDay -> Color.White.copy(alpha = 0.85f)
                            else -> Color.Transparent
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isToday) {
                                    Modifier.background(
                                        Color(0xFF03DAC5).copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}
