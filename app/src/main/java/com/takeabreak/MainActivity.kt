package com.takeabreak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.takeabreak.platform.workers.WorkScheduler
import com.takeabreak.ui.navigation.AppNavHost
import com.takeabreak.ui.theme.TakeABreakTheme

// singleTop en el manifest: las notificaciones con la app abierta llegan a onNewIntent, que escucha AppNavHost
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WorkScheduler.syncBreaksNow(this)
        setContent {
            TakeABreakTheme {
                AppNavHost()
            }
        }
    }
}
