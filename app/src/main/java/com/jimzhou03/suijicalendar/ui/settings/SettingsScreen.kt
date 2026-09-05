package com.jimzhou03.suijicalendar.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jimzhou03.suijicalendar.ui.SuijiViewModel
import com.jimzhou03.suijicalendar.backup.BackupPayload
import com.jimzhou03.suijicalendar.backup.ImportMode
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SuijiViewModel) {
    val context = LocalContext.current
    val masterEnabled by viewModel.remindersEnabled.collectAsState()
    fun permissionGranted() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(permissionGranted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var pendingImport by remember { mutableStateOf<BackupPayload?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it) { result -> status = result.fold({ "备份已导出" }, { "导出失败：${it.message}" }) } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.inspectBackup(it) { result ->
            result.onSuccess { pendingImport = it }.onFailure { status = "读取失败：${it.message}" }
        } }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("设置") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("提醒", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("启用本地提醒")
                    Text("由 WorkManager 调度，可能受省电策略影响而延迟。", style = MaterialTheme.typography.bodySmall)
                }
                Switch(masterEnabled, onCheckedChange = viewModel::setRemindersEnabled)
            }
            if (!granted && Build.VERSION.SDK_INT >= 33) {
                Text("通知权限尚未授予；拒绝权限不会影响日历和清单。")
                Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("允许通知") }
            } else {
                Text("通知权限：已允许", color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Text("隐私", style = MaterialTheme.typography.titleLarge)
            Text("所有纪念日和清单默认只保存在本机。应用不申请联网、联系人、系统日历或存储权限，也不包含统计和追踪 SDK。")
            HorizontalDivider()
            Text("备份与恢复", style = MaterialTheme.typography.titleLarge)
            Text("版本化 JSON 通过系统文件选择器读写，不需要存储权限。导入会先预览数量。")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { exportLauncher.launch("岁记日历-${LocalDate.now()}.json") }) { Text("导出备份") }
                Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) { Text("导入备份") }
            }
            status?.let { Text(it, color = if (it.contains("失败")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        }
    }
    pendingImport?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("确认导入备份") },
            text = {
                Text("纪念日 ${payload.commemorations.size} 条，清单系列 ${payload.taskSeries.size} 条，状态记录 ${payload.taskOccurrences.size} 条。安全合并会跳过重复项目；覆盖恢复会清空当前数据后写入。")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importBackup(payload, ImportMode.MERGE) { result ->
                        status = result.fold({ "备份已安全合并" }, { "导入失败：${it.message}" })
                    }
                    pendingImport = null
                }) { Text("安全合并") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingImport = null }) { Text("取消") }
                    TextButton(onClick = {
                        viewModel.importBackup(payload, ImportMode.REPLACE) { result ->
                            status = result.fold({ "已覆盖恢复" }, { "恢复失败：${it.message}" })
                        }
                        pendingImport = null
                    }) { Text("覆盖恢复") }
                }
            },
        )
    }
}
