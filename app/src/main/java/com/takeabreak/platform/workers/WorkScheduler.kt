package com.takeabreak.platform.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Todo el trabajo en segundo plano pasa por WorkManager, sin servicios en primer plano (RNF01)
object WorkScheduler {

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedulePeriodic(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            "break-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BreakSyncWorker>(15, TimeUnit.MINUTES).build(),
        )
        workManager.enqueueUniquePeriodicWork(
            "catalog-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CatalogSyncWorker>(6, TimeUnit.HOURS).setConstraints(online).build(),
        )
    }

    // Al abrir la app también se buscan pausas nuevas
    fun syncBreaksNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "break-sync-now",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BreakSyncWorker>().build(),
        )
    }

    // REPLACE: si ya había uno esperando la red, el nuevo manda los totales más recientes.
    // Se puede cortar a la mitad porque reenviar un día no suma dos veces
    fun syncPoints(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "points-sync",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PointsSyncWorker>()
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }
}
