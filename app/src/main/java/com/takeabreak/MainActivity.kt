package com.takeabreak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.takeabreak.di.appContainer
import com.takeabreak.platform.workers.WorkScheduler
import com.takeabreak.ui.navigation.AppNavHost
import com.takeabreak.ui.theme.TakeABreakTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WorkScheduler.syncBreaksNow(this)
        val signedIn = appContainer.observeSession()
        setContent {
            TakeABreakTheme {
                AppNavHost(signedIn)
            }
        }
    }
}
