package com.takeabreak.platform.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.takeabreak.di.appContainer
import java.io.IOException

class BreakSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.appContainer.breakSync()
        return Result.success()
    }
}

class CatalogSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = retryOnNetworkError {
        applicationContext.appContainer.getRewards.refresh()
    }
}

class RedemptionSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = retryOnNetworkError {
        applicationContext.appContainer.redeemReward.syncPending()
    }
}

// Sin red se reintenta con espera exponencial
private suspend fun retryOnNetworkError(block: suspend () -> Unit): Result =
    try {
        block()
        Result.success()
    } catch (_: IOException) {
        Result.retry()
    }
