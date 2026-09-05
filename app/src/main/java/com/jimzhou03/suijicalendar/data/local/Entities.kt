package com.jimzhou03.suijicalendar.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "commemorations")
data class CommemorationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val originalSolarEpochDay: Long,
    val originalBasis: String,
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val lunarLeapMonth: Boolean,
    val note: String = "",
    val colorArgb: Int = 0xFF8B4B62.toInt(),
    val enableSolarTrack: Boolean = true,
    val enableLunarTrack: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderAdvanceDays: Int = 7,
    val reminderMinutesOfDay: Int = 9 * 60,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "task_series")
data class TaskSeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val anchorEpochDay: Long,
    val recurrence: String,
    val weekDays: String = "",
    val reminderMinutesOfDay: Int? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "task_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = TaskSeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("seriesId"), Index("displayEpochDay")],
)
data class TaskOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: Long,
    val scheduledEpochDay: Long,
    val displayEpochDay: Long,
    val state: String,
    val completedAt: Long? = null,
    val linkGroupId: String? = null,
    val migrationCopy: Boolean = false,
)
