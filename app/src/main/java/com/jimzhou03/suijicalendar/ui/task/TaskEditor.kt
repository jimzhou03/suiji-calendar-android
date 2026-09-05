package com.jimzhou03.suijicalendar.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jimzhou03.suijicalendar.core.model.RecurrenceRule
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import java.time.LocalDate

@Composable
fun TaskEditor(
    date: LocalDate,
    editing: TaskSeriesEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, RecurrenceRule, Set<Int>, Int?) -> Unit,
) {
    var title by remember(editing) { mutableStateOf(editing?.title.orEmpty()) }
    var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
    var dateText by remember(editing) {
        mutableStateOf(editing?.let { LocalDate.ofEpochDay(it.anchorEpochDay).toString() } ?: date.toString())
    }
    var recurrence by remember(editing) {
        mutableStateOf(editing?.recurrence?.let { RecurrenceRule.valueOf(it) } ?: RecurrenceRule.NONE)
    }
    var weekDays by remember(editing) {
        mutableStateOf(editing?.weekDays?.split(',')?.mapNotNull(String::toIntOrNull)?.toSet() ?: setOf(date.dayOfWeek.value))
    }
    var reminderText by remember(editing) {
        mutableStateOf(editing?.reminderMinutesOfDay?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "新增清单事项" else "编辑清单事项") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("事项") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dateText, { dateText = it; error = null }, label = { Text("开始日期 YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceRule.entries.forEach { rule ->
                        FilterChip(selected = recurrence == rule, onClick = { recurrence = rule }, label = { Text(rule.label) })
                    }
                }
                if (recurrence == RecurrenceRule.WEEKLY) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, label ->
                            val value = index + 1
                            FilterChip(
                                selected = value in weekDays,
                                onClick = { weekDays = if (value in weekDays) weekDays - value else weekDays + value },
                                label = { Text(label) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    reminderText,
                    { reminderText = it; error = null },
                    label = { Text("提醒 HH:mm（留空则不提醒）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDate = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                val reminder = if (reminderText.isBlank()) null else parseTime(reminderText)
                when {
                    title.isBlank() -> error = "请填写事项"
                    parsedDate == null -> error = "日期格式应为 YYYY-MM-DD"
                    parsedDate.year !in 1901..2100 -> error = "仅支持 1901—2100 年"
                    recurrence == RecurrenceRule.WEEKLY && weekDays.isEmpty() -> error = "每周重复至少选择一天"
                    reminderText.isNotBlank() && reminder == null -> error = "提醒时间格式应为 HH:mm"
                    else -> onSave(title, note, parsedDate, recurrence, weekDays, reminder)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun parseTime(value: String): Int? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}
