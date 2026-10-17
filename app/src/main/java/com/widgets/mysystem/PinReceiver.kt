package com.widgets.mysystem

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.runBlocking

/** Called by the system after the user confirms adding a pinned widget; assigns the chosen design to it. */
class PinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val name = Store.prefs(context).getString("pending", null)
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID && name != null) {
            Store.assign(context, id, name)
            Store.prefs(context).edit().remove("pending").apply()
        }
        val app = context.applicationContext
        val pr = goAsync()
        Thread {
            try { runBlocking { GenericWidget().updateAll(app) } } finally { pr.finish() }
        }.start()
    }
}
