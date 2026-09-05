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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.jimzhou03.suijicalendar.core.model.CommemorationType
import com.jimzhou03.suijicalendar.data.CommemorationOccurrence
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import com.jimzhou03.suijicalendar.ui.SuijiViewModel
import com.jimzhou03.suijicalendar.ui.commemoration.CommemorationEditor
import com.jimzhou03.suijicalendar.ui.task.TaskEditor
import com.jimzhou03.suijicalendar.ui.task.TaskRow
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
fun CalendarScreen(viewModel: SuijiViewModel) {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var editorTarget by remember { mutableStateOf<CommemorationEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showTaskEditor by remember { mutableStateOf(false) }
    var taskEditorTarget by remember { mutableStateOf<TaskSeriesEntity?>(null) }
    val commemorations by viewModel.commemorations.collectAsState()
    val series by viewModel.taskSeries.collectAsState()
    val taskOccurrences by viewModel.taskOccurrences.collectAsState()
    val selectedOccurrences = remember(selectedDate, commemorations) {
        viewModel.repository.occurrencesOn(selectedDate, commemorations)
    }
    val selectedTasks = remember(selectedDate, series, taskOccurrences) { viewModel.tasksOn(selectedDate) }
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
                    IconButton(onClick = { selectedDate = today }) { Icon(Icons.Outlined.Today, "回到今天") }
                },
                actions = {
                    FilledTonalIconButton(onClick = { editorTarget = null; showEditor = true }) {
                        Icon(Icons.Outlined.Add, "新增纪念日")
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
                    val occurrences = remember(day.date, commemorations) {
                        if (day.position == DayPosition.MonthDate) viewModel.repository.occurrencesOn(day.date, commemorations) else emptyList()
                    }
                    val dayTasks = remember(day.date, series, taskOccurrences) {
                        if (day.position == DayPosition.MonthDate) viewModel.tasksOn(day.date) else emptyList()
                    }
                    DayCell(
                        day = day,
                        selected = day.date == selectedDate,
                        today = day.date == today,
                        occurrences = occurrences,
                        taskProgress = dayTasks.count { it.state == com.jimzhou03.suijicalendar.core.model.TaskState.COMPLETED } to dayTasks.size,
                        onClick = { selectedDate = day.date },
                    )
                },
            )
            HorizontalDivider()
            DayDetails(
                date = selectedDate,
                occurrences = selectedOccurrences,
                tasks = selectedTasks,
                onAdd = { editorTarget = null; showEditor = true },
                onAddTask = { taskEditorTarget = null; showTaskEditor = true },
                onEdit = { editorTarget = it; showEditor = true },
                onDelete = viewModel::deleteCommemoration,
                onToggleTask = viewModel::setTaskCompleted,
                onMoveTask = viewModel::moveTaskToToday,
                onEditTask = { taskEditorTarget = it; showTaskEditor = true },
                onDeleteTask = viewModel::deleteTask,
            )
        }
    }

    if (showEditor) {
        CommemorationEditor(
            initialDate = selectedDate,
            editing = editorTarget,
            onDismiss = { showEditor = false },
            onSave = { name, type, basis, date, leap, note, solar, lunar, countMode, annual, reminder, advance, minutes ->
                viewModel.saveCommemoration(editorTarget, name, type, basis, date, leap, note, solar, lunar, countMode, annual, reminder, advance, minutes)
                showEditor = false
            },
        )
    }
    if (showTaskEditor) {
        TaskEditor(selectedDate, taskEditorTarget, { showTaskEditor = false }) { title, note, date, recurrence, days, reminder ->
            viewModel.saveTask(taskEditorTarget, title, note, date, recurrence, days, reminder)
            showTaskEditor = false
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY).forEach { day ->
            Text(day.getDisplayName(TextStyle.NARROW, Locale.CHINA), Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selected: Boolean,
    today: Boolean,
    occurrences: List<CommemorationOccurrence>,
    taskProgress: Pair<Int, Int>,
    onClick: () -> Unit,
) {
    val inMonth = day.position == DayPosition.MonthDate
    Column(
        modifier = Modifier.aspectRatio(0.82f).padding(2.dp).clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = inMonth, onClick = onClick).padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            day.date.dayOfMonth.toString(),
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            color = if (!inMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            else if (today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (inMonth) {
            Text(runCatching { CalendarEngine.lunarLabel(day.date) }.getOrDefault(""), fontSize = 10.sp, maxLines = 1)
            Row {
                occurrences.distinctBy { it.commemoration.id }.take(3).forEach { occurrence ->
                    Spacer(
                        Modifier.padding(1.dp).size(5.dp).clip(CircleShape)
                            .background(Color(occurrence.commemoration.colorArgb)),
                    )
                }
            }
            if (taskProgress.second > 0) {
                Text("${taskProgress.first}/${taskProgress.second}", fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun DayDetails(
    date: LocalDate,
    occurrences: List<CommemorationOccurrence>,
    tasks: List<com.jimzhou03.suijicalendar.core.task.TaskItem>,
    onAdd: () -> Unit,
    onAddTask: () -> Unit,
    onEdit: (CommemorationEntity) -> Unit,
    onDelete: (CommemorationEntity) -> Unit,
    onToggleTask: (com.jimzhou03.suijicalendar.core.task.TaskItem, Boolean) -> Unit,
    onMoveTask: (com.jimzhou03.suijicalendar.core.task.TaskItem) -> Unit,
    onEditTask: (TaskSeriesEntity) -> Unit,
    onDeleteTask: (TaskSeriesEntity) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)), style = MaterialTheme.typography.titleMedium)
                    Text("农历${CalendarEngine.solarToLunar(date).displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, "为当天新增纪念日") }
            }
        }
        if (occurrences.isEmpty()) {
            item { Text("当天暂无纪念日，下方清单会在下一阶段接入。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        occurrences.forEach { occurrence ->
            item(key = "${occurrence.commemoration.id}-${occurrence.resolved.track}") {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(occurrence.commemoration) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.size(10.dp).clip(CircleShape).background(Color(occurrence.commemoration.colorArgb)))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(occurrence.commemoration.name, fontWeight = FontWeight.Medium)
                        Text(
                            CommemorationType.valueOf(occurrence.commemoration.type).label +
                                if (occurrence.resolved.adjusted) " · 本年日期已调整" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    AssistChip(onClick = { onEdit(occurrence.commemoration) }, label = { Text(occurrence.resolved.track.label) })
                    IconButton(onClick = { onDelete(occurrence.commemoration) }) { Icon(Icons.Outlined.Delete, "删除") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text("每日清单", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onAddTask) { Icon(Icons.Outlined.Add, "新增清单事项") }
            }
        }
        if (tasks.isEmpty()) item { Text("当天暂无清单事项。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        tasks.forEach { task ->
            item(key = "task-${task.occurrenceId ?: "${task.series.id}-${task.scheduledDate}"}") {
                TaskRow(
                    item = task,
                    shownDate = date,
                    onToggle = { onToggleTask(task, it) },
                    onMoveToday = { onMoveTask(task) },
                    onEdit = { onEditTask(task.series) },
                    onDelete = { onDeleteTask(task.series) },
                )
            }
        }
    }
}
