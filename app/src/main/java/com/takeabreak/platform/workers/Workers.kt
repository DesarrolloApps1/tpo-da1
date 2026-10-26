package com.takeabreak.platform.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.takeabreak.di.appContainer
import kotlinx.coroutines.flow.first
import java.io.IOException

// 1. lee los eventos, 2. valida y guarda, 3. avisa si sumó
class BreakSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val pauses = container.usageEventsReader.closedPausesSince(container.settings.checkpoint())
        val earned = container.syncBreaks(pauses).filter { it.points > 0 }
        if (earned.isEmpty()) return Result.success()
        // Con las pausas ya guardadas, el saldo dice si ya alcanza para la meta
        val summary = container.getHomeSummary().first()
        val reachedGoal = summary.goal?.takeIf { summary.balance >= it.points }
        earned.forEach { container.notifier.breakValidated(it, reachedGoal) }
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
