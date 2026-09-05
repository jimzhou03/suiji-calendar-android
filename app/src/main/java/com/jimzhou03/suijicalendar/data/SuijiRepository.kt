package com.jimzhou03.suijicalendar.data

import androidx.room.withTransaction
import com.jimzhou03.suijicalendar.core.date.CalendarEngine
import com.jimzhou03.suijicalendar.core.date.ResolvedDate
import com.jimzhou03.suijicalendar.core.model.OccurrenceTrack
import com.jimzhou03.suijicalendar.data.local.AppDatabase
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import java.time.LocalDate

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
