package com.widgets.mysystem

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val conn = URL("https://example.com/api/widget.json").openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            applicationContext.getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
                .putString("value", json.optString("value", "--"))
                .putString("subtitle", json.optString("subtitle", ""))
                .apply()
            Widget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
