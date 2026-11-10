package com.takeabreak.platform.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.ServerException
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

class PointsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = retryOnNetworkError {
        applicationContext.appContainer.syncPoints()
    }
}

// Sin red o con el servidor caído se reintenta con espera exponencial. Un 4xx va a volver a fallar igual
private suspend fun retryOnNetworkError(block: suspend () -> Unit): Result =
    try {
        block()
        Result.success()
    } catch (_: IOException) {
        Result.retry()
    } catch (e: ServerException) {
        if (e.isRetryable) Result.retry() else Result.failure()
    }
