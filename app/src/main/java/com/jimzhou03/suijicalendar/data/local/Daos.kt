package com.jimzhou03.suijicalendar.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CommemorationDao {
    @Query("SELECT * FROM commemorations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CommemorationEntity>>

    @Query("SELECT * FROM commemorations ORDER BY createdAt DESC")
    suspend fun getAll(): List<CommemorationEntity>

    @Upsert
    suspend fun upsert(item: CommemorationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CommemorationEntity>): List<Long>

    @Delete
    suspend fun delete(item: CommemorationEntity)

    @Query("DELETE FROM commemorations")
    suspend fun deleteAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_series WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeSeries(): Flow<List<TaskSeriesEntity>>

    @Query("SELECT * FROM task_occurrences")
    fun observeOccurrences(): Flow<List<TaskOccurrenceEntity>>

    @Query("SELECT * FROM task_series")
    suspend fun getAllSeries(): List<TaskSeriesEntity>

    @Query("SELECT * FROM task_occurrences")
    suspend fun getAllOccurrences(): List<TaskOccurrenceEntity>

    @Upsert
    suspend fun upsertSeries(item: TaskSeriesEntity): Long

    @Insert
    suspend fun insertOccurrence(item: TaskOccurrenceEntity): Long

    @Upsert
    suspend fun upsertOccurrence(item: TaskOccurrenceEntity): Long

    @Delete
    suspend fun deleteSeries(item: TaskSeriesEntity)

    @Query("DELETE FROM task_occurrences")
    suspend fun deleteAllOccurrences()

    @Query("DELETE FROM task_series")
    suspend fun deleteAllSeries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSeries(items: List<TaskSeriesEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOccurrences(items: List<TaskOccurrenceEntity>): List<Long>
}
