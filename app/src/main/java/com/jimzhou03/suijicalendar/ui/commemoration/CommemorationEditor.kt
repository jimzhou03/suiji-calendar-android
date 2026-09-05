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
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import java.time.LocalDate

@Composable
fun CommemorationEditor(
    initialDate: LocalDate,
    editing: CommemorationEntity?,
    onDismiss: () -> Unit,
    onSave: (String, CommemorationType, CalendarBasis, LocalDate, Boolean, String, Boolean, Boolean) -> Unit,
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
                ToggleRow("每年标注公历日期", solarTrack) { solarTrack = it }
                ToggleRow("每年标注农历日期", lunarTrack) { lunarTrack = it }
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
                Text("默认提前7天及当天09:00提醒，可在提醒设置中修改。")
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                when {
                    name.isBlank() -> error = "请填写名称"
                    parsed == null -> error = "日期格式应为 YYYY-MM-DD"
                    parsed.year !in 1901..2100 -> error = "仅支持 1901—2100 年"
                    !solarTrack && !lunarTrack -> error = "至少启用一条年度轨道"
                    else -> onSave(name, type, basis, parsed, leapMonth, note, solarTrack, lunarTrack)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
