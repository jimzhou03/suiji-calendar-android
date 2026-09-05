package com.jimzhou03.suijicalendar.data

import androidx.room.withTransaction
import com.jimzhou03.suijicalendar.core.date.CalendarEngine
import com.jimzhou03.suijicalendar.core.date.ResolvedDate
import com.jimzhou03.suijicalendar.core.model.OccurrenceTrack
import com.jimzhou03.suijicalendar.core.model.TaskState
import com.jimzhou03.suijicalendar.core.task.TaskItem
import com.jimzhou03.suijicalendar.data.local.AppDatabase
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import java.time.LocalDate
import java.util.UUID

data class CommemorationOccurrence(
    val commemoration: CommemorationEntity,
    val resolved: ResolvedDate,
)

class SuijiRepository(private val database: AppDatabase) {
    private val commemorationsDao = database.commemorationDao()
    private val taskDao = database.taskDao()

    val commemorations = commemorationsDao.observeAll()
    val taskSeries = taskDao.observeSeries()
    val taskOccurrences = taskDao.observeOccurrences()

    suspend fun saveCommemoration(item: CommemorationEntity) = commemorationsDao.upsert(item)
    suspend fun deleteCommemoration(item: CommemorationEntity) = commemorationsDao.delete(item)
    suspend fun saveTaskSeries(item: com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity) = taskDao.upsertSeries(item)
    suspend fun deleteTaskSeries(item: com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity) = taskDao.deleteSeries(item)

    suspend fun setTaskCompleted(item: TaskItem, completed: Boolean) {
        val state = if (completed) TaskState.COMPLETED else TaskState.PENDING
        taskDao.upsertOccurrence(
            com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity(
                id = item.occurrenceId ?: 0,
                seriesId = item.series.id,
                scheduledEpochDay = item.scheduledDate.toEpochDay(),
                displayEpochDay = item.displayDate.toEpochDay(),
                state = state.name,
                completedAt = if (completed) System.currentTimeMillis() else null,
                linkGroupId = item.linkGroupId,
                migrationCopy = item.migrationCopy,
            ),
        )
    }

    suspend fun moveTaskToDate(item: TaskItem, target: LocalDate) = database.withTransaction {
        require(item.state == TaskState.PENDING) { "只能迁移待完成事项" }
        require(item.displayDate != target) { "事项已在目标日期" }
        val link = item.linkGroupId ?: UUID.randomUUID().toString()
        taskDao.upsertOccurrence(
            com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity(
                id = item.occurrenceId ?: 0,
                seriesId = item.series.id,
                scheduledEpochDay = item.scheduledDate.toEpochDay(),
                displayEpochDay = item.displayDate.toEpochDay(),
                state = TaskState.MOVED.name,
                linkGroupId = link,
                migrationCopy = item.migrationCopy,
            ),
        )
        taskDao.insertOccurrence(
            com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity(
                seriesId = item.series.id,
                scheduledEpochDay = item.scheduledDate.toEpochDay(),
                displayEpochDay = target.toEpochDay(),
                state = TaskState.PENDING.name,
                linkGroupId = link,
                migrationCopy = true,
            ),
        )
    }

    fun occurrencesForYear(item: CommemorationEntity, year: Int): List<CommemorationOccurrence> {
        val results = mutableListOf<CommemorationOccurrence>()
        if (item.enableSolarTrack) {
            val original = LocalDate.ofEpochDay(item.originalSolarEpochDay)
            results += CommemorationOccurrence(item, CalendarEngine.resolveSolarAnniversary(original, year))
        }
        if (item.enableLunarTrack) {
            (year - 1..year).forEach { lunarYear ->
                if (lunarYear in CalendarEngine.MIN_YEAR..CalendarEngine.MAX_YEAR) {
                    val resolved = CalendarEngine.lunarToSolar(
                        year = lunarYear,
                        month = item.lunarMonth,
                        day = item.lunarDay,
                        isLeapMonth = item.lunarLeapMonth,
                    )
                    if (resolved.date.year == year) results += CommemorationOccurrence(item, resolved)
                }
            }
        }
        return results.distinctBy { it.resolved.track to it.resolved.date }
    }

    fun occurrencesOn(date: LocalDate, items: List<CommemorationEntity>): List<CommemorationOccurrence> =
        items.flatMap { occurrencesForYear(it, date.year) }.filter { it.resolved.date == date }

    fun nextOccurrences(
        item: CommemorationEntity,
        from: LocalDate = LocalDate.now(),
    ): List<CommemorationOccurrence> = (from.year..(from.year + 2).coerceAtMost(CalendarEngine.MAX_YEAR))
        .flatMap { occurrencesForYear(item, it) }
        .filter { !it.resolved.date.isBefore(from) }
        .sortedBy { it.resolved.date }

    suspend fun replaceEverything(
        commemorations: List<CommemorationEntity>,
        series: List<com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity>,
        occurrences: List<com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity>,
    ) = database.withTransaction {
        taskDao.deleteAllOccurrences()
        taskDao.deleteAllSeries()
        commemorationsDao.deleteAll()
        commemorationsDao.insertAll(commemorations)
        taskDao.insertAllSeries(series)
        taskDao.insertAllOccurrences(occurrences)
    }
}
