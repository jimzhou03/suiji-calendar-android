package com.jimzhou03.suijicalendar.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import com.jimzhou03.suijicalendar.ui.SuijiViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: SuijiViewModel) {
    val today = remember { LocalDate.now() }
    val series by viewModel.taskSeries.collectAsState()
    val occurrences by viewModel.taskOccurrences.collectAsState()
    val tasks = remember(series, occurrences, today) { viewModel.tasksOn(today) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskSeriesEntity?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("今日 · ${today.format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA))}") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) { Icon(Icons.Outlined.Add, "新增今日事项") }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (tasks.isEmpty()) item { Text("今天还没有事项。", modifier = Modifier.padding(top = 24.dp)) }
            items(tasks.size, key = { tasks[it].occurrenceId ?: "${tasks[it].series.id}-${tasks[it].scheduledDate}" }) { index ->
                val item = tasks[index]
                TaskRow(
                    item = item,
                    shownDate = today,
                    onToggle = { viewModel.setTaskCompleted(item, it) },
                    onMoveToday = {},
                    onEdit = { editing = item.series; showEditor = true },
                    onDelete = { viewModel.deleteTask(item.series) },
                )
            }
        }
    }
    if (showEditor) {
        TaskEditor(today, editing, { showEditor = false }) { title, note, date, recurrence, days, reminder ->
            viewModel.saveTask(editing, title, note, date, recurrence, days, reminder)
            showEditor = false
        }
    }
}
