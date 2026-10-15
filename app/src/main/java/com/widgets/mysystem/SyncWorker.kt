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
        val specs = Store.specs(applicationContext)
        for (n in specs.keys()) {
            val url = specs.getJSONObject(n).optJSONObject("source")?.optString("url") ?: ""
            if (url.isBlank()) continue
            try { Store.saveData(applicationContext, n, Store.http(url)) } catch (e: Exception) { }
        }
        GenericWidget().updateAll(applicationContext)
        return Result.success()
    }
}
