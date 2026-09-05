package com.jimzhou03.suijicalendar

import android.app.Application
import com.jimzhou03.suijicalendar.data.SuijiRepository
import com.jimzhou03.suijicalendar.data.local.AppDatabase

class SuijiApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { SuijiRepository(database) }
}
