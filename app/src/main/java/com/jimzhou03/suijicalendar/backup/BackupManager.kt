package com.jimzhou03.suijicalendar.backup

import android.content.Context
import android.net.Uri
import com.jimzhou03.suijicalendar.data.SuijiRepository
import com.jimzhou03.suijicalendar.data.local.CommemorationEntity
import com.jimzhou03.suijicalendar.data.local.TaskOccurrenceEntity
import com.jimzhou03.suijicalendar.data.local.TaskSeriesEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val version: Int,
    val createdAt: Long,
    val commemorations: List<CommemorationEntity>,
    val taskSeries: List<TaskSeriesEntity>,
    val taskOccurrences: List<TaskOccurrenceEntity>,
)

enum class ImportMode { MERGE, REPLACE }

class BackupManager(
    private val context: Context,
    private val repository: SuijiRepository,
) {
    suspend fun exportTo(uri: Uri) {
        val (commemorations, series, occurrences) = repository.snapshot()
        val payload = BackupPayload(CURRENT_VERSION, System.currentTimeMillis(), commemorations, series, occurrences)
        checkNotNull(context.contentResolver.openOutputStream(uri, "w")) { "无法打开目标文件" }
            .bufferedWriter(Charsets.UTF_8).use { it.write(BackupCodec.encode(payload)) }
    }

    fun readFrom(uri: Uri): BackupPayload {
        val text = checkNotNull(context.contentResolver.openInputStream(uri)) { "无法打开备份文件" }
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        return BackupCodec.decode(text)
    }

    suspend fun import(payload: BackupPayload, mode: ImportMode) {
        when (mode) {
            ImportMode.REPLACE -> repository.replaceEverything(payload.commemorations, payload.taskSeries, payload.taskOccurrences)
            ImportMode.MERGE -> repository.mergeEverything(payload.commemorations, payload.taskSeries, payload.taskOccurrences)
        }
    }

    companion object { const val CURRENT_VERSION = 1 }
}

object BackupCodec {
    fun encode(payload: BackupPayload): String = JSONObject()
        .put("format", "suiji-calendar-backup")
        .put("version", payload.version)
        .put("createdAt", payload.createdAt)
        .put("commemorations", JSONArray().apply { payload.commemorations.forEach { put(it.toJson()) } })
        .put("taskSeries", JSONArray().apply { payload.taskSeries.forEach { put(it.toJson()) } })
        .put("taskOccurrences", JSONArray().apply { payload.taskOccurrences.forEach { put(it.toJson()) } })
        .toString(2)

    fun decode(text: String): BackupPayload {
        val root = JSONObject(text)
        require(root.optString("format") == "suiji-calendar-backup") { "不是岁记日历备份" }
        val version = root.getInt("version")
        require(version == BackupManager.CURRENT_VERSION) { "不支持的备份版本：$version" }
        return BackupPayload(
            version = version,
            createdAt = root.getLong("createdAt"),
            commemorations = root.getJSONArray("commemorations").mapObjects(::commemorationFromJson),
            taskSeries = root.getJSONArray("taskSeries").mapObjects(::seriesFromJson),
            taskOccurrences = root.getJSONArray("taskOccurrences").mapObjects(::occurrenceFromJson),
        )
    }
}

private fun CommemorationEntity.toJson() = JSONObject()
    .put("id", id).put("name", name).put("type", type)
    .put("originalSolarEpochDay", originalSolarEpochDay).put("originalBasis", originalBasis)
    .put("lunarYear", lunarYear).put("lunarMonth", lunarMonth).put("lunarDay", lunarDay)
    .put("lunarLeapMonth", lunarLeapMonth).put("note", note).put("colorArgb", colorArgb)
    .put("enableSolarTrack", enableSolarTrack).put("enableLunarTrack", enableLunarTrack)
    .put("reminderEnabled", reminderEnabled).put("reminderAdvanceDays", reminderAdvanceDays)
    .put("reminderMinutesOfDay", reminderMinutesOfDay).put("customCountMode", customCountMode)
    .put("annual", annual).put("createdAt", createdAt)

private fun TaskSeriesEntity.toJson() = JSONObject()
    .put("id", id).put("title", title).put("note", note).put("anchorEpochDay", anchorEpochDay)
    .put("recurrence", recurrence).put("weekDays", weekDays)
    .put("reminderMinutesOfDay", reminderMinutesOfDay ?: JSONObject.NULL)
    .put("archived", archived).put("createdAt", createdAt)

private fun TaskOccurrenceEntity.toJson() = JSONObject()
    .put("id", id).put("seriesId", seriesId).put("scheduledEpochDay", scheduledEpochDay)
    .put("displayEpochDay", displayEpochDay).put("state", state)
    .put("completedAt", completedAt ?: JSONObject.NULL).put("linkGroupId", linkGroupId ?: JSONObject.NULL)
    .put("migrationCopy", migrationCopy)

private fun commemorationFromJson(o: JSONObject) = CommemorationEntity(
    id = o.getLong("id"), name = o.getString("name"), type = o.getString("type"),
    originalSolarEpochDay = o.getLong("originalSolarEpochDay"), originalBasis = o.getString("originalBasis"),
    lunarYear = o.getInt("lunarYear"), lunarMonth = o.getInt("lunarMonth"), lunarDay = o.getInt("lunarDay"),
    lunarLeapMonth = o.getBoolean("lunarLeapMonth"), note = o.optString("note"), colorArgb = o.getInt("colorArgb"),
    enableSolarTrack = o.getBoolean("enableSolarTrack"), enableLunarTrack = o.getBoolean("enableLunarTrack"),
    reminderEnabled = o.getBoolean("reminderEnabled"), reminderAdvanceDays = o.getInt("reminderAdvanceDays"),
    reminderMinutesOfDay = o.getInt("reminderMinutesOfDay"), customCountMode = o.optString("customCountMode", "COUNTDOWN"),
    annual = o.optBoolean("annual", true), createdAt = o.getLong("createdAt"),
)

private fun seriesFromJson(o: JSONObject) = TaskSeriesEntity(
    id = o.getLong("id"), title = o.getString("title"), note = o.optString("note"),
    anchorEpochDay = o.getLong("anchorEpochDay"), recurrence = o.getString("recurrence"),
    weekDays = o.optString("weekDays"), reminderMinutesOfDay = if (o.isNull("reminderMinutesOfDay")) null else o.getInt("reminderMinutesOfDay"),
    archived = o.optBoolean("archived"), createdAt = o.getLong("createdAt"),
)

private fun occurrenceFromJson(o: JSONObject) = TaskOccurrenceEntity(
    id = o.getLong("id"), seriesId = o.getLong("seriesId"), scheduledEpochDay = o.getLong("scheduledEpochDay"),
    displayEpochDay = o.getLong("displayEpochDay"), state = o.getString("state"),
    completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt"),
    linkGroupId = if (o.isNull("linkGroupId")) null else o.getString("linkGroupId"),
    migrationCopy = o.optBoolean("migrationCopy"),
)

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
