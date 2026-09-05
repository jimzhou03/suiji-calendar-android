package com.jimzhou03.suijicalendar.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jimzhou03.suijicalendar.MainActivity
import com.jimzhou03.suijicalendar.SuijiApplication
import com.jimzhou03.suijicalendar.core.model.TaskState
import com.jimzhou03.suijicalendar.core.task.TaskEngine
import com.jimzhou03.suijicalendar.core.task.TaskItem
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CountdownWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 120.dp), DpSize(250.dp, 120.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as SuijiApplication).repository
        val today = LocalDate.now()
        val items = repository.commemorations.first().mapNotNull { event ->
            repository.nextOccurrences(event, today).firstOrNull()?.let { occurrence ->
                Triple(event.name, occurrence.resolved.date, ChronoUnit.DAYS.between(today, occurrence.resolved.date))
            }
        }.sortedBy { it.second }
        provideContent { CountdownWidgetContent(items) }
    }
}

@Composable
private fun CountdownWidgetContent(items: List<Triple<String, LocalDate, Long>>) {
    val count = if (LocalSize.current.width >= 220.dp) 3 else 1
    GlanceTheme {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground)
                .padding(14.dp).clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("岁记 · 倒数日", style = TextStyle(fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.size(8.dp))
            if (items.isEmpty()) Text("打开 App 添加重要日子")
            items.take(count).forEach { item ->
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(item.first, modifier = GlanceModifier.defaultWeight())
                    Text(if (item.third == 0L) "今天" else "${item.third}天")
                }
            }
        }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

private val occurrenceIdKey = ActionParameters.Key<Long>("occurrence_id")
private val seriesIdKey = ActionParameters.Key<Long>("series_id")
private val scheduledDayKey = ActionParameters.Key<Long>("scheduled_day")
private val migrationCopyKey = ActionParameters.Key<Boolean>("migration_copy")
private val completedKey = ActionParameters.Key<Boolean>("completed")

class TodayTaskWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(250.dp, 120.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as SuijiApplication).repository
        val series = repository.taskSeries.first()
        val occurrences = repository.taskOccurrences.first()
        val tasks = TaskEngine.tasksOn(LocalDate.now(), series, occurrences)
        provideContent { TodayTaskWidgetContent(tasks) }
    }
}

@Composable
private fun TodayTaskWidgetContent(tasks: List<TaskItem>) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).padding(12.dp),
        ) {
            Row(GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>())) {
                Text("今日清单", modifier = GlanceModifier.defaultWeight(), style = TextStyle(fontWeight = FontWeight.Bold))
                Text("${tasks.count { it.state == TaskState.COMPLETED }}/${tasks.size}")
            }
            if (tasks.isEmpty()) Text("今天没有事项")
            tasks.take(5).forEach { item ->
                val nextCompleted = item.state != TaskState.COMPLETED
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp).clickable(
                        actionRunCallback<ToggleTaskWidgetAction>(
                            actionParametersOf(
                                occurrenceIdKey to (item.occurrenceId ?: 0L),
                                seriesIdKey to item.series.id,
                                scheduledDayKey to item.scheduledDate.toEpochDay(),
                                migrationCopyKey to item.migrationCopy,
                                completedKey to nextCompleted,
                            ),
                        ),
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (item.state == TaskState.COMPLETED) "✓" else "○")
                    Spacer(GlanceModifier.size(8.dp))
                    Text(item.series.title, maxLines = 1)
                }
            }
        }
    }
}

class TodayTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTaskWidget()
}

class ToggleTaskWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as SuijiApplication
        val seriesId = parameters[seriesIdKey] ?: return
        val scheduled = parameters[scheduledDayKey] ?: return
        val series = app.repository.taskSeries.first().firstOrNull { it.id == seriesId } ?: return
        val occurrenceId = parameters[occurrenceIdKey]?.takeIf { it != 0L }
        val item = TaskItem(
            series = series,
            scheduledDate = LocalDate.ofEpochDay(scheduled),
            displayDate = LocalDate.now(),
            state = if (parameters[completedKey] == true) TaskState.PENDING else TaskState.COMPLETED,
            occurrenceId = occurrenceId,
            migrationCopy = parameters[migrationCopyKey] == true,
        )
        app.repository.setTaskCompleted(item, parameters[completedKey] == true)
        TodayTaskWidget().updateAll(context)
    }
}

suspend fun refreshWidgets(context: Context) {
    CountdownWidget().updateAll(context)
    TodayTaskWidget().updateAll(context)
}
