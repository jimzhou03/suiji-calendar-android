package com.jimzhou03.suijicalendar.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jimzhou03.suijicalendar.SuijiApplication
import com.jimzhou03.suijicalendar.core.task.TaskEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    private val manager = WorkManager.getInstance(context)

    suspend fun rebuild() {
        val app = context.applicationContext as SuijiApplication
        if (!app.settingsStore.remindersEnabled.first()) {
            manager.cancelAllWorkByTag(REMINDER_TAG)
            return
        }
        manager.cancelAllWorkByTag(REMINDER_TAG)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val events = app.repository.commemorations.first()
        events.filter { it.reminderEnabled }.forEach { event ->
            app.repository.nextOccurrences(event, today).take(2).forEach { occurrence ->
                setOf(event.reminderAdvanceDays, 0).forEach { advance ->
                    val date = occurrence.resolved.date.minusDays(advance.toLong())
                    val at = LocalDateTime.of(date, minutesToTime(event.reminderMinutesOfDay))
                    if (at.isAfter(now)) {
                        enqueue(
                            uniqueName = "event-${event.id}-${occurrence.resolved.track}-$date-$advance",
                            at = at,
                            title = if (advance == 0) "今天是${event.name}" else "${event.name}还有${advance}天",
                            body = "${occurrence.resolved.track.label}日期 ${occurrence.resolved.date}",
                        )
                    }
                }
            }
        }
        val series = app.repository.taskSeries.first()
        val occurrences = app.repository.taskOccurrences.first()
        series.filter { it.reminderMinutesOfDay != null }.forEach { task ->
            (0L..90L).map { today.plusDays(it) }.filter { TaskEngine.isScheduled(task, it) }.forEach { date ->
                val item = TaskEngine.tasksOn(date, listOf(task), occurrences).firstOrNull()
                if (item != null && item.state.name == "PENDING") {
                    val at = LocalDateTime.of(date, minutesToTime(task.reminderMinutesOfDay!!))
                    if (at.isAfter(now)) enqueue("task-${task.id}-$date", at, task.title, "今日清单提醒")
                }
            }
        }
    }

    private fun enqueue(uniqueName: String, at: LocalDateTime, title: String, body: String) {
        val instant = at.atZone(ZoneId.systemDefault()).toInstant()
        val delay = Duration.between(java.time.Instant.now(), instant).toMillis().coerceAtLeast(0)
        val data = Data.Builder().putString("title", title).putString("body", body)
            .putInt("notification_id", uniqueName.hashCode()).build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(REMINDER_TAG)
            .build()
        manager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun minutesToTime(minutes: Int) = LocalTime.of(minutes / 60, minutes % 60)

    companion object { const val REMINDER_TAG = "suiji-reminders" }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { ReminderScheduler(context).rebuild() }
            pending.finish()
        }
    }
}
