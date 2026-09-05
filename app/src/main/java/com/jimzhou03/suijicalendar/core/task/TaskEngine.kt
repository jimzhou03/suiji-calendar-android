package com.jimzhou03.suijicalendar.core.task

import com.jimzhou03.suijicalendar.core.model.RecurrenceRule
import com.jimzhou03.suijicalendar.core.model.TaskState
import com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import java.time.LocalDate
import java.time.YearMonth

data class TaskItem(
    val series: TaskSeriesEntity,
    val scheduledDate: LocalDate,
    val displayDate: LocalDate,
    val state: TaskState,
    val occurrenceId: Long? = null,
    val migrationCopy: Boolean = false,
    val linkGroupId: String? = null,
)

object TaskEngine {
    fun isScheduled(series: TaskSeriesEntity, date: LocalDate): Boolean {
        val anchor = LocalDate.ofEpochDay(series.anchorEpochDay)
        if (date.isBefore(anchor) || series.archived) return false
        return when (RecurrenceRule.valueOf(series.recurrence)) {
            RecurrenceRule.NONE -> date == anchor
            RecurrenceRule.DAILY -> true
            RecurrenceRule.WEEKLY -> {
                val days = series.weekDays.split(',').mapNotNull(String::toIntOrNull).toSet()
                date.dayOfWeek.value in (days.ifEmpty { setOf(anchor.dayOfWeek.value) })
            }
            RecurrenceRule.MONTHLY -> date.dayOfMonth == anchor.dayOfMonth.coerceAtMost(YearMonth.from(date).lengthOfMonth())
            RecurrenceRule.YEARLY -> {
                date.month == anchor.month &&
                    date.dayOfMonth == anchor.dayOfMonth.coerceAtMost(YearMonth.from(date).lengthOfMonth())
            }
        }
    }

    fun tasksOn(
        date: LocalDate,
        series: List<TaskSeriesEntity>,
        occurrences: List<TaskOccurrenceEntity>,
    ): List<TaskItem> {
        val base = series.filter { isScheduled(it, date) }.map { taskSeries ->
            val occurrence = occurrences.firstOrNull {
                it.seriesId == taskSeries.id &&
                    it.scheduledEpochDay == date.toEpochDay() &&
                    !it.migrationCopy
            }
            TaskItem(
                series = taskSeries,
                scheduledDate = date,
                displayDate = date,
                state = occurrence?.state?.let(TaskState::valueOf) ?: TaskState.PENDING,
                occurrenceId = occurrence?.id,
                linkGroupId = occurrence?.linkGroupId,
            )
        }
        val migrated = occurrences.filter { it.migrationCopy && it.displayEpochDay == date.toEpochDay() }
            .mapNotNull { occurrence ->
                series.firstOrNull { it.id == occurrence.seriesId }?.let {
                    TaskItem(
                        series = it,
                        scheduledDate = LocalDate.ofEpochDay(occurrence.scheduledEpochDay),
                        displayDate = date,
                        state = TaskState.valueOf(occurrence.state),
                        occurrenceId = occurrence.id,
                        migrationCopy = true,
                        linkGroupId = occurrence.linkGroupId,
                    )
                }
            }
        return (base + migrated).sortedWith(compareBy<TaskItem> { it.state == TaskState.COMPLETED }.thenBy { it.series.createdAt })
    }
}
