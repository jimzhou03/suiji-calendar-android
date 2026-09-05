package com.jimzhou03.suijicalendar.backup

import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val payload = BackupPayload(
        version = 1,
        createdAt = 123,
        commemorations = listOf(
            CommemorationEntity(
                id = 1, name = "虚构人物生日", type = "BIRTHDAY", originalSolarEpochDay = 12_000,
                originalBasis = "SOLAR", lunarYear = 2002, lunarMonth = 11, lunarDay = 4,
                lunarLeapMonth = false,
            ),
        ),
        taskSeries = listOf(TaskSeriesEntity(id = 2, title = "测试", anchorEpochDay = 20_000, recurrence = "NONE")),
        taskOccurrences = listOf(
            TaskOccurrenceEntity(id = 3, seriesId = 2, scheduledEpochDay = 20_000, displayEpochDay = 20_000, state = "COMPLETED"),
        ),
    )

    @Test
    fun `backup round trip preserves records`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(payload))
        assertEquals(payload, decoded)
    }

    @Test
    fun `corrupt json is rejected`() {
        assertThrows(Exception::class.java) { BackupCodec.decode("not-json") }
    }

    @Test
    fun `unknown version is rejected`() {
        val text = BackupCodec.encode(payload).replace("\"version\": 1", "\"version\": 99")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(text) }
    }
}
