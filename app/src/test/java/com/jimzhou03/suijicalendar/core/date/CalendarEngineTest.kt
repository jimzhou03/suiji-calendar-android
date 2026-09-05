package com.jimzhou03.suijicalendar.core.date

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarEngineTest {
    @Test
    fun `2003-06-30 is lunar sixth month first day`() {
        val lunar = CalendarEngine.solarToLunar(LocalDate.of(2003, 6, 30))
        assertEquals(2003, lunar.year)
        assertEquals(6, lunar.month)
        assertEquals(1, lunar.day)
        assertFalse(lunar.isLeapMonth)
    }

    @Test
    fun `2003 birthday has expected two tracks in 2026`() {
        val solarTrack = CalendarEngine.resolveSolarAnniversary(LocalDate.of(2003, 6, 30), 2026)
        val lunarTrack = CalendarEngine.lunarToSolar(2026, 6, 1, false)
        assertEquals(LocalDate.of(2026, 6, 30), solarTrack.date)
        assertEquals(LocalDate.of(2026, 7, 14), lunarTrack.date)
    }

    @Test
    fun `solar leap day clamps to last day of non leap February`() {
        val result = CalendarEngine.resolveSolarAnniversary(LocalDate.of(2024, 2, 29), 2025)
        assertEquals(LocalDate.of(2025, 2, 28), result.date)
        assertTrue(result.adjusted)
    }

    @Test
    fun `missing lunar leap month falls back and reports adjustment`() {
        val result = CalendarEngine.lunarToSolar(2026, 6, 1, true)
        assertEquals(LocalDate.of(2026, 7, 14), result.date)
        assertTrue(result.adjusted)
    }

    @Test
    fun `lunar day thirty clamps in a short month`() {
        val shortMonth = (1..12).first { month ->
            runCatching {
                val day29 = CalendarEngine.lunarToSolar(2026, month, 29, false)
                val day30 = CalendarEngine.lunarToSolar(2026, month, 30, false)
                day29.date == day30.date && day30.adjusted
            }.getOrDefault(false)
        }
        val result = CalendarEngine.lunarToSolar(2026, shortMonth, 30, false)
        assertTrue(result.adjusted)
    }
}
