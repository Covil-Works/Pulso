package com.covildev.pulso.feature_registro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthCalendar(
    month: YearMonth,
    highlightedDays: Set<Int>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoNextMonth: Boolean,
    onDayClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"))
    val firstWeekDay = month.atDay(1).dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val totalSlots = firstWeekDay + daysInMonth
    val rows = (totalSlots + 6) / 7

    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
            }
            Text(
                text = month.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = onNextMonth,
                enabled = canGoNextMonth,
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK_DAYS.forEach { weekDay ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = weekDay,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val slotIndex = row * 7 + column
                    val dayNumber = slotIndex - firstWeekDay + 1
                    val isValidDay = dayNumber in 1..daysInMonth
                    val isHighlighted = dayNumber in highlightedDays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isValidDay) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .then(
                                        if (isHighlighted) {
                                            Modifier.clickable { onDayClick(dayNumber) }
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .then(
                                        if (isHighlighted) {
                                            Modifier.border(
                                                border = BorderStroke(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                ),
                                                shape = CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val WEEK_DAYS = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom")
