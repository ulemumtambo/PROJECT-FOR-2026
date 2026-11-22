package com.reportit.malawi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.reportit.malawi.navigation.ReportItMalawiApp
import com.reportit.malawi.ui.theme.ReportItMalawiTheme

/**
 * MainActivity: I created MainActivity as the single activity entry point for ReportIt Malawi,
 * initializing Jetpack Compose, the application theme, and launching the root navigation graph.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.reportit.malawi.data.ReportStore.init(applicationContext)
        setContent {
            ReportItMalawiTheme {
                ReportItMalawiApp()
            }
        }
    }
}
