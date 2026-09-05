package com.jimzhou03.suijicalendar.ui.commemoration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jimzhou03.suijicalendar.core.model.CalendarBasis
import com.jimzhou03.suijicalendar.core.model.CommemorationType
import com.jimzhou03.suijicalendar.core.model.CustomCountMode
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import java.time.LocalDate

@Composable
fun CommemorationEditor(
    initialDate: LocalDate,
    editing: CommemorationEntity?,
    onDismiss: () -> Unit,
    onSave: (String, CommemorationType, CalendarBasis, LocalDate, Boolean, String, Boolean, Boolean, CustomCountMode, Boolean, Boolean, Int, Int) -> Unit,
) {
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) {
        mutableStateOf(editing?.type?.let { runCatching { CommemorationType.valueOf(it) }.getOrNull() } ?: CommemorationType.BIRTHDAY)
    }
    var basis by remember(editing) {
        mutableStateOf(editing?.originalBasis?.let { runCatching { CalendarBasis.valueOf(it) }.getOrNull() } ?: CalendarBasis.SOLAR)
    }
    val initialText = editing?.let {
        if (basis == CalendarBasis.SOLAR) LocalDate.ofEpochDay(it.originalSolarEpochDay).toString()
        else "%04d-%02d-%02d".format(it.lunarYear, it.lunarMonth, it.lunarDay)
    } ?: initialDate.toString()
    var dateText by remember(editing) { mutableStateOf(initialText) }
    var leapMonth by remember(editing) { mutableStateOf(editing?.lunarLeapMonth ?: false) }
    var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
    var solarTrack by remember(editing) { mutableStateOf(editing?.enableSolarTrack ?: true) }
    var lunarTrack by remember(editing) { mutableStateOf(editing?.enableLunarTrack ?: true) }
    var countMode by remember(editing) {
        mutableStateOf(editing?.customCountMode?.let { CustomCountMode.valueOf(it) } ?: CustomCountMode.COUNTDOWN)
    }
    var annual by remember(editing) { mutableStateOf(editing?.annual ?: true) }
    var reminderEnabled by remember(editing) { mutableStateOf(editing?.reminderEnabled ?: true) }
    var advanceText by remember(editing) { mutableStateOf((editing?.reminderAdvanceDays ?: 7).toString()) }
    var reminderTime by remember(editing) {
        val minutes = editing?.reminderMinutesOfDay ?: 9 * 60
        mutableStateOf("%02d:%02d".format(minutes / 60, minutes % 60))
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "新增纪念日" else "编辑纪念日") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommemorationType.entries.forEach {
                        FilterChip(selected = type == it, onClick = { type = it }, label = { Text(it.label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = basis == CalendarBasis.SOLAR, onClick = { basis = CalendarBasis.SOLAR }, label = { Text("公历输入") })
                    FilterChip(selected = basis == CalendarBasis.LUNAR, onClick = { basis = CalendarBasis.LUNAR }, label = { Text("农历输入") })
                }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it; error = null },
                    label = { Text(if (basis == CalendarBasis.SOLAR) "公历日期 YYYY-MM-DD" else "农历日期 YYYY-MM-DD") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (basis == CalendarBasis.LUNAR) {
                    ToggleRow("闰月", leapMonth) { leapMonth = it }
                }
                if (type == CommemorationType.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomCountMode.entries.forEach { mode ->
                            FilterChip(selected = countMode == mode, onClick = { countMode = mode }, label = { Text(mode.label) })
                        }
                    }
                    ToggleRow("每年重复", annual) { annual = it }
                }
                ToggleRow("每年标注公历日期", solarTrack) { solarTrack = it }
                ToggleRow("每年标注农历日期", lunarTrack) { lunarTrack = it }
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
                ToggleRow("启用本地提醒", reminderEnabled) { reminderEnabled = it }
                if (reminderEnabled) {
                    OutlinedTextField(advanceText, { advanceText = it; error = null }, label = { Text("提前天数") }, singleLine = true)
                    OutlinedTextField(reminderTime, { reminderTime = it; error = null }, label = { Text("提醒时间 HH:mm") }, singleLine = true)
                    Text("将同时在提前日与当天提醒；系统省电策略可能造成延迟。")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                val advance = advanceText.toIntOrNull()
                val minutes = parseMinutes(reminderTime)
                when {
                    name.isBlank() -> error = "请填写名称"
                    parsed == null -> error = "日期格式应为 YYYY-MM-DD"
                    parsed.year !in 1901..2100 -> error = "仅支持 1901—2100 年"
                    !solarTrack && !lunarTrack -> error = "至少启用一条年度轨道"
                    reminderEnabled && (advance == null || advance !in 0..365) -> error = "提前天数应为 0—365"
                    reminderEnabled && minutes == null -> error = "提醒时间格式应为 HH:mm"
                    else -> onSave(name, type, basis, parsed, leapMonth, note, solarTrack, lunarTrack, countMode, annual, reminderEnabled, advance ?: 7, minutes ?: 9 * 60)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun parseMinutes(value: String): Int? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
