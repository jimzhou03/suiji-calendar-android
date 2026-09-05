package com.jimzhou03.suijicalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimzhou03.suijicalendar.core.date.CalendarEngine
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    val state = rememberCalendarState(
        startMonth = YearMonth.of(CalendarEngine.MIN_YEAR, 1),
        endMonth = YearMonth.of(CalendarEngine.MAX_YEAR, 12),
        firstVisibleMonth = YearMonth.from(today),
        firstDayOfWeek = DayOfWeek.MONDAY,
    )
    val visibleMonth by remember { derivedStateOf { state.firstVisibleMonth.yearMonth } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(visibleMonth.format(DateTimeFormatter.ofPattern("yyyy年 M月")), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { selectedDate = today }) {
                        Icon(Icons.Outlined.Today, contentDescription = "回到今天")
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { selectedDate = today }) {
                        Icon(Icons.Outlined.Add, contentDescription = "新增")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            WeekHeader()
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    DayCell(
                        day = day,
                        selected = day.date == selectedDate,
                        today = day.date == today,
                        onClick = { selectedDate = day.date },
                    )
                },
            )
            SelectedDaySummary(selectedDate)
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY).forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, Locale.CHINA),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, selected: Boolean, today: Boolean, onClick: () -> Unit) {
    val inMonth = day.position == DayPosition.MonthDate
    Column(
        modifier = Modifier.aspectRatio(0.82f).padding(2.dp).clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = inMonth, onClick = onClick).padding(top = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            color = when {
                !inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                today -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (inMonth) {
            Text(
                text = runCatching { CalendarEngine.lunarLabel(day.date) }.getOrDefault(""),
                fontSize = 10.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (today) {
                Spacer(Modifier.size(3.dp))
                Spacer(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun SelectedDaySummary(date: LocalDate) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)), style = MaterialTheme.typography.titleMedium)
        Text(
            text = runCatching { "农历${CalendarEngine.solarToLunar(date).displayName}" }.getOrDefault("超出支持范围"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("点击日期后，这里将显示纪念日与每日清单", style = MaterialTheme.typography.bodySmall)
    }
}
