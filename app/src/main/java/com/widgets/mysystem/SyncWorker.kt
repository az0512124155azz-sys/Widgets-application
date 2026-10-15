package com.widgets.mysystem

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import java.util.concurrent.TimeUnit

object Sync {
    fun schedule(c: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(c).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class SyncWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        try { Store.sync(applicationContext) } catch (e: Exception) { }
        Store.refreshData(applicationContext)
        GenericWidget().updateAll(applicationContext)
        return Result.success()
    }
}
