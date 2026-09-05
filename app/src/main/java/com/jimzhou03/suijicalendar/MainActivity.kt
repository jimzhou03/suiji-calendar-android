package com.jimzhou03.suijicalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.jimzhou03.suijicalendar.ui.SuijiApp
import com.jimzhou03.suijicalendar.ui.theme.SuijiCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuijiCalendarTheme {
                SuijiApp()
            }
        }
    }
}
