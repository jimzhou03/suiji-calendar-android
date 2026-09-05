package com.jimzhou03.suijicalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jimzhou03.suijicalendar.ui.theme.SuijiCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SuijiCalendarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun AppPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("岁记日历", style = MaterialTheme.typography.headlineLarge)
        Text("把重要的日子，年年记得")
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPlaceholderPreview() = SuijiCalendarTheme { AppPlaceholder() }
