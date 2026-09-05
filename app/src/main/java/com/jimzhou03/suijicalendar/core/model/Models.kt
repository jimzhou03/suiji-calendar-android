package com.jimzhou03.suijicalendar.core.model

enum class CalendarBasis { SOLAR, LUNAR }

enum class CommemorationType(val label: String) {
    BIRTHDAY("生日"), DEATH_DAY("忌日"), ANNIVERSARY("纪念日"), CUSTOM("自定义")
}

enum class OccurrenceTrack(val label: String) { SOLAR("公历"), LUNAR("农历") }

enum class RecurrenceRule(val label: String) {
    NONE("不重复"), DAILY("每天"), WEEKLY("每周"), MONTHLY("每月"), YEARLY("每年")
}

enum class TaskState { PENDING, COMPLETED, MOVED }

enum class CustomCountMode(val label: String) { COUNTDOWN("倒数"), COUNTUP("累计") }
