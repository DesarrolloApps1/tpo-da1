package com.takeabreak

import android.app.Application
import com.takeabreak.di.AppContainer
import com.takeabreak.platform.workers.WorkScheduler

class TakeABreakApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WorkScheduler.schedulePeriodic(this)
    }
}
