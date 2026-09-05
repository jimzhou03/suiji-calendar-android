package com.jimzhou03.suijicalendar.core.task

import com.jimzhou03.suijicalendar.core.model.RecurrenceRule
import com.jimzhou03.suijicalendar.core.model.TaskState
import com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskEngineTest {
    private fun series(anchor: LocalDate, rule: RecurrenceRule, weekDays: String = "") = TaskSeriesEntity(
        id = 7,
        title = "测试事项",
        anchorEpochDay = anchor.toEpochDay(),
        recurrence = rule.name,
        weekDays = weekDays,
    )

    @Test
    fun `monthly day 31 clamps to month end`() {
        val task = series(LocalDate.of(2026, 1, 31), RecurrenceRule.MONTHLY)
        assertTrue(TaskEngine.isScheduled(task, LocalDate.of(2026, 2, 28)))
        assertFalse(TaskEngine.isScheduled(task, LocalDate.of(2026, 2, 27)))
    }

    @Test
    fun `yearly leap day clamps to February 28`() {
        val task = series(LocalDate.of(2024, 2, 29), RecurrenceRule.YEARLY)
        assertTrue(TaskEngine.isScheduled(task, LocalDate.of(2025, 2, 28)))
    }

    @Test
    fun `weekly rule honors selected weekdays`() {
        val task = series(LocalDate.of(2026, 9, 1), RecurrenceRule.WEEKLY, "1,3,5")
        assertTrue(TaskEngine.isScheduled(task, LocalDate.of(2026, 9, 2)))
        assertFalse(TaskEngine.isScheduled(task, LocalDate.of(2026, 9, 3)))
    }

    @Test
    fun `move retains original history and creates linked target`() {
        val original = LocalDate.of(2026, 9, 1)
        val target = LocalDate.of(2026, 9, 5)
        val task = series(original, RecurrenceRule.NONE)
        val records = listOf(
            TaskOccurrenceEntity(
                id = 1, seriesId = task.id, scheduledEpochDay = original.toEpochDay(),
                displayEpochDay = original.toEpochDay(), state = TaskState.MOVED.name,
                linkGroupId = "same", migrationCopy = false,
            ),
            TaskOccurrenceEntity(
                id = 2, seriesId = task.id, scheduledEpochDay = original.toEpochDay(),
                displayEpochDay = target.toEpochDay(), state = TaskState.PENDING.name,
                linkGroupId = "same", migrationCopy = true,
            ),
        )
        val oldItems = TaskEngine.tasksOn(original, listOf(task), records)
        val newItems = TaskEngine.tasksOn(target, listOf(task), records)
        assertEquals(TaskState.MOVED, oldItems.single().state)
        assertEquals(TaskState.PENDING, newItems.single().state)
        assertEquals(oldItems.single().linkGroupId, newItems.single().linkGroupId)
    }

    @Test
    fun `completing one daily occurrence does not affect the next`() {
        val start = LocalDate.of(2026, 9, 1)
        val task = series(start, RecurrenceRule.DAILY)
        val completed = TaskOccurrenceEntity(
            id = 1, seriesId = task.id, scheduledEpochDay = start.toEpochDay(),
            displayEpochDay = start.toEpochDay(), state = TaskState.COMPLETED.name,
        )
        assertEquals(TaskState.COMPLETED, TaskEngine.tasksOn(start, listOf(task), listOf(completed)).single().state)
        assertEquals(TaskState.PENDING, TaskEngine.tasksOn(start.plusDays(1), listOf(task), listOf(completed)).single().state)
    }
}
