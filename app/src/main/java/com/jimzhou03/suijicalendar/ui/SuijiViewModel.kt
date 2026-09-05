package com.jimzhou03.suijicalendar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jimzhou03.suijicalendar.SuijiApplication
import com.jimzhou03.suijicalendar.core.date.CalendarEngine
import com.jimzhou03.suijicalendar.core.model.CalendarBasis
import com.jimzhou03.suijicalendar.core.model.CommemorationType
import com.jimzhou03.suijicalendar.core.model.CustomCountMode
import com.jimzhou03.suijicalendar.core.model.RecurrenceRule
import com.jimzhou03.suijicalendar.core.task.TaskEngine
import com.jimzhou03.suijicalendar.core.task.TaskItem
import com.jimzhou03.suijicalendar.data.CommemorationOccurrence
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SuijiViewModel(application: Application) : AndroidViewModel(application) {
    val repository = (application as SuijiApplication).repository
    val commemorations = repository.commemorations.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val taskSeries = repository.taskSeries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val taskOccurrences = repository.taskOccurrences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun occurrencesOn(date: LocalDate): List<CommemorationOccurrence> =
        repository.occurrencesOn(date, commemorations.value)

    fun tasksOn(date: LocalDate): List<TaskItem> = TaskEngine.tasksOn(date, taskSeries.value, taskOccurrences.value)

    fun saveCommemoration(
        editing: CommemorationEntity?,
        name: String,
        type: CommemorationType,
        basis: CalendarBasis,
        date: LocalDate,
        lunarLeapMonth: Boolean,
        note: String,
        solarTrack: Boolean,
        lunarTrack: Boolean,
        countMode: CustomCountMode,
        annual: Boolean,
    ) {
        val lunar = if (basis == CalendarBasis.SOLAR) {
            CalendarEngine.solarToLunar(date)
        } else {
            val solar = CalendarEngine.lunarToSolar(date.year, date.monthValue, date.dayOfMonth, lunarLeapMonth).date
            CalendarEngine.solarToLunar(solar)
        }
        val solar = if (basis == CalendarBasis.SOLAR) date else {
            CalendarEngine.lunarToSolar(date.year, date.monthValue, date.dayOfMonth, lunarLeapMonth).date
        }
        val color = when (type) {
            CommemorationType.BIRTHDAY -> 0xFFB7557B.toInt()
            CommemorationType.DEATH_DAY -> 0xFF59636E.toInt()
            CommemorationType.ANNIVERSARY -> 0xFF3F7F76.toInt()
            CommemorationType.CUSTOM -> 0xFF9A6A2F.toInt()
        }
        viewModelScope.launch {
            repository.saveCommemoration(
                CommemorationEntity(
                    id = editing?.id ?: 0,
                    name = name.trim(),
                    type = type.name,
                    originalSolarEpochDay = solar.toEpochDay(),
                    originalBasis = basis.name,
                    lunarYear = lunar.year,
                    lunarMonth = lunar.month,
                    lunarDay = lunar.day,
                    lunarLeapMonth = if (basis == CalendarBasis.LUNAR) lunarLeapMonth else lunar.isLeapMonth,
                    note = note.trim(),
                    colorArgb = color,
                    enableSolarTrack = solarTrack,
                    enableLunarTrack = lunarTrack,
                    reminderEnabled = editing?.reminderEnabled ?: true,
                    reminderAdvanceDays = editing?.reminderAdvanceDays ?: 7,
                    reminderMinutesOfDay = editing?.reminderMinutesOfDay ?: 9 * 60,
                    customCountMode = countMode.name,
                    annual = annual,
                    createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteCommemoration(item: CommemorationEntity) {
        viewModelScope.launch { repository.deleteCommemoration(item) }
    }

    fun saveTask(
        editing: TaskSeriesEntity?,
        title: String,
        note: String,
        date: LocalDate,
        recurrence: RecurrenceRule,
        weekDays: Set<Int>,
        reminderMinutes: Int?,
    ) {
        viewModelScope.launch {
            repository.saveTaskSeries(
                TaskSeriesEntity(
                    id = editing?.id ?: 0,
                    title = title.trim(),
                    note = note.trim(),
                    anchorEpochDay = date.toEpochDay(),
                    recurrence = recurrence.name,
                    weekDays = weekDays.sorted().joinToString(","),
                    reminderMinutesOfDay = reminderMinutes,
                    archived = editing?.archived ?: false,
                    createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    fun setTaskCompleted(item: TaskItem, completed: Boolean) {
        viewModelScope.launch { repository.setTaskCompleted(item, completed) }
    }

    fun moveTaskToToday(item: TaskItem) {
        viewModelScope.launch { repository.moveTaskToDate(item, LocalDate.now()) }
    }

    fun deleteTask(item: TaskSeriesEntity) {
        viewModelScope.launch { repository.deleteTaskSeries(item) }
    }
}
