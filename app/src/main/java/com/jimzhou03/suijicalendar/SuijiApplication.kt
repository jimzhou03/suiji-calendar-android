package com.jimzhou03.suijicalendar

import android.app.Application
import com.jimzhou03.suijicalendar.data.SuijiRepository
import com.jimzhou03.suijicalendar.data.local.AppDatabase
import com.jimzhou03.suijicalendar.reminder.ReminderScheduler
import com.jimzhou03.suijicalendar.reminder.createReminderChannel
import com.jimzhou03.suijicalendar.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SuijiApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { SuijiRepository(database) }
    val settingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        createReminderChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            ReminderScheduler(this@SuijiApplication).rebuild()
        }
    }
}
