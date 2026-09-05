package com.jimzhou03.suijicalendar.ui.countdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jimzhou03.suijicalendar.core.date.CalendarEngine
import com.jimzhou03.suijicalendar.core.model.CustomCountMode
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import com.jimzhou03.suijicalendar.ui.SuijiViewModel
import com.jimzhou03.suijicalendar.ui.commemoration.CommemorationEditor
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownScreen(viewModel: SuijiViewModel) {
    val events by viewModel.commemorations.collectAsState()
    val today = remember { LocalDate.now() }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CommemorationEntity?>(null) }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("倒数日") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) { Icon(Icons.Outlined.Add, "新增倒数日") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (events.isEmpty()) item { Text("还没有重要日子，点右下角开始记录。", modifier = Modifier.padding(top = 24.dp)) }
            items(events, key = { it.id }) { event ->
                CountdownCard(event, today, viewModel, onClick = { editing = event; showEditor = true })
            }
        }
    }
    if (showEditor) {
        CommemorationEditor(today, editing, { showEditor = false }) { name, type, basis, date, leap, note, solar, lunar, countMode, annual, reminder, advance, minutes ->
            viewModel.saveCommemoration(editing, name, type, basis, date, leap, note, solar, lunar, countMode, annual, reminder, advance, minutes)
            showEditor = false
        }
    }
}

@Composable
private fun CountdownCard(
    event: CommemorationEntity,
    today: LocalDate,
    viewModel: SuijiViewModel,
    onClick: () -> Unit,
) {
    val occurrences = remember(event, today) { viewModel.repository.nextOccurrences(event, today) }
    val original = LocalDate.ofEpochDay(event.originalSolarEpochDay)
    val countMode = CustomCountMode.valueOf(event.customCountMode)
    val primaryDate = if (!event.annual) original else occurrences.firstOrNull()?.resolved?.date
    val rawDays = primaryDate?.let { ChronoUnit.DAYS.between(today, it) } ?: 0L
    val count = if (!event.annual && countMode == CustomCountMode.COUNTUP) {
        ChronoUnit.DAYS.between(original, today).coerceAtLeast(0)
    } else kotlin.math.abs(rawDays)
    val wording = when {
        !event.annual && countMode == CustomCountMode.COUNTUP -> "已经"
        rawDays >= 0 -> "还有"
        else -> "已经"
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(event.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (event.note.isNotBlank()) Text(event.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("$wording $count 天", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (event.annual) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    occurrences.groupBy { it.resolved.track }.values.mapNotNull { it.firstOrNull() }.forEach { occurrence ->
                        AssistChip(
                            onClick = onClick,
                            label = {
                                val adjusted = if (occurrence.resolved.adjusted) " · 已调整" else ""
                                Text("${occurrence.resolved.track.label} ${occurrence.resolved.date}$adjusted")
                            },
                        )
                    }
                }
            } else {
                val lunar = runCatching { CalendarEngine.solarToLunar(original).displayName }.getOrDefault("")
                Text("公历 $original · 农历$lunar", style = MaterialTheme.typography.bodySmall)
            }
            Text("点击卡片可编辑日期与规则", style = MaterialTheme.typography.labelSmall)
        }
    }
}
