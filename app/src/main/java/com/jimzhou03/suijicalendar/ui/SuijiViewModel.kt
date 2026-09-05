package com.jimzhou03.suijicalendar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jimzhou03.suijicalendar.SuijiApplication
import com.jimzhou03.suijicalendar.core.date.CalendarEngine
import com.jimzhou03.suijicalendar.core.model.CalendarBasis
import com.jimzhou03.suijicalendar.core.model.CommemorationType
import com.jimzhou03.suijicalendar.data.CommemorationOccurrence
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
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

    fun occurrencesOn(date: LocalDate): List<CommemorationOccurrence> =
        repository.occurrencesOn(date, commemorations.value)

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
                    createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteCommemoration(item: CommemorationEntity) {
        viewModelScope.launch { repository.deleteCommemoration(item) }
    }
}
