package com.jimzhou03.suijicalendar.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jimzhou03.suijicalendar.core.model.TaskState
import com.jimzhou03.suijicalendar.core.task.TaskItem
import java.time.LocalDate

@Composable
fun TaskRow(
    item: TaskItem,
    shownDate: LocalDate,
    onToggle: (Boolean) -> Unit,
    onMoveToday: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.state == TaskState.COMPLETED,
            onCheckedChange = if (item.state == TaskState.MOVED) null else onToggle,
        )
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = item.series.title,
                textDecoration = if (item.state == TaskState.COMPLETED) TextDecoration.LineThrough else null,
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                item.state == TaskState.MOVED -> Text("已迁移到今天", style = MaterialTheme.typography.bodySmall)
                item.migrationCopy -> Text("由 ${item.scheduledDate} 迁入", style = MaterialTheme.typography.bodySmall)
                item.series.note.isNotBlank() -> Text(item.series.note, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (item.state == TaskState.PENDING && shownDate != LocalDate.now()) {
            IconButton(onClick = onMoveToday) { Icon(Icons.Outlined.DriveFileMove, "移到今天") }
        }
        IconButton(onClick = onEdit) { Text("编") }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "删除事项") }
    }
}
