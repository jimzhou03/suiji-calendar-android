package com.jimzhou03.suijicalendar.core.date

import com.jimzhou03.suijicalendar.core.model.OccurrenceTrack
import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.solar.SolarDay
import java.time.LocalDate
import java.time.YearMonth

data class LunarDateValue(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean,
    val displayName: String,
)

data class ResolvedDate(
    val date: LocalDate,
    val track: OccurrenceTrack,
    val adjusted: Boolean = false,
)

object CalendarEngine {
    const val MIN_YEAR = 1901
    const val MAX_YEAR = 2100

    fun requireSupported(date: LocalDate) {
        require(date.year in MIN_YEAR..MAX_YEAR) { "仅支持 $MIN_YEAR—$MAX_YEAR" }
    }

    fun solarToLunar(date: LocalDate): LunarDateValue {
        requireSupported(date)
        val lunar = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth).getLunarDay()
        return LunarDateValue(
            year = lunar.year,
            month = kotlin.math.abs(lunar.month),
            day = lunar.day,
            isLeapMonth = lunar.getLunarMonth().isLeap(),
            displayName = lunar.getName(),
        )
    }

    fun lunarLabel(date: LocalDate): String {
        val lunar = solarToLunar(date)
        return if (lunar.day == 1) {
            (if (lunar.isLeapMonth) "闰" else "") + lunarMonthName(lunar.month)
        } else lunar.displayName
    }

    fun lunarToSolar(year: Int, month: Int, day: Int, isLeapMonth: Boolean): ResolvedDate {
        require(year in MIN_YEAR..MAX_YEAR) { "仅支持 $MIN_YEAR—$MAX_YEAR" }
        require(month in 1..12) { "农历月份无效" }
        require(day in 1..30) { "农历日期无效" }
        var adjusted = false
        val requestedMonth = if (isLeapMonth) -month else month
        val lunarMonth = runCatching { LunarMonth.fromYm(year, requestedMonth) }.getOrElse {
            adjusted = true
            LunarMonth.fromYm(year, month)
        }
        if (isLeapMonth && !lunarMonth.isLeap()) adjusted = true
        val actualDay = day.coerceAtMost(lunarMonth.getDayCount())
        if (actualDay != day) adjusted = true
        val solar = LunarDay.fromYmd(year, lunarMonth.getMonthWithLeap(), actualDay).getSolarDay()
        return ResolvedDate(
            date = LocalDate.of(solar.year, solar.month, solar.day),
            track = OccurrenceTrack.LUNAR,
            adjusted = adjusted,
        )
    }

    fun resolveSolarAnniversary(original: LocalDate, year: Int): ResolvedDate {
        val day = original.dayOfMonth.coerceAtMost(YearMonth.of(year, original.month).lengthOfMonth())
        return ResolvedDate(
            date = LocalDate.of(year, original.month, day),
            track = OccurrenceTrack.SOLAR,
            adjusted = day != original.dayOfMonth,
        )
    }

    fun lunarMonthName(month: Int): String = when (month) {
        1 -> "正月"; 2 -> "二月"; 3 -> "三月"; 4 -> "四月"; 5 -> "五月"; 6 -> "六月"
        7 -> "七月"; 8 -> "八月"; 9 -> "九月"; 10 -> "十月"; 11 -> "冬月"; 12 -> "腊月"
        else -> error("农历月份无效")
    }
}
