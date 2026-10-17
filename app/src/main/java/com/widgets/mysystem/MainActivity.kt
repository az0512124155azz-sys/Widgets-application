package com.widgets.mysystem

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.runBlocking

class MainActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val ctx = this
        val app = applicationContext
        Sync.schedule(ctx)
        Store.seedDemo(ctx)
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 96, 40, 40) }
        val link = EditText(ctx).apply { hint = "Paste your link here"; setText(Store.link(ctx)) }
        val connect = Button(ctx).apply { text = "Connect and sync" }
        val status = TextView(ctx).apply { setPadding(0, 16, 0, 16) }
        val designs = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        fun pin(name: String) {
            Store.prefs(ctx).edit().putString("pending", name).apply()
            val mgr = AppWidgetManager.getInstance(ctx)
            if (mgr.isRequestPinAppWidgetSupported) {
                val cb = PendingIntent.getBroadcast(
                    ctx, 0, Intent(ctx, PinReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                mgr.requestPinAppWidget(ComponentName(ctx, Receiver::class.java), null, cb)
            } else status.text = "Long-press the home screen, choose Widgets and add this app."
        }

        fun refresh() {
            designs.removeAllViews()
            designs.addView(TextView(ctx).apply { text = "Tap a design to add it as a new widget:"; setPadding(0, 16, 0, 8) })
            for (n in Store.specs(ctx).keys().asSequence().toList()) {
                designs.addView(Button(ctx).apply {
                    text = "Add widget: " + n
                    setOnClickListener { pin(n) }
                })
            }
        }

        fun doSync(save: Boolean) {
            status.text = "Syncing..."
            val text = link.text.toString()
            Thread {
                val msg = try {
                    if (save) Store.saveLink(app, text)
                    Store.sync(app)
                    Store.refreshData(app)
                    runBlocking { GenericWidget().updateAll(app) }
                    "Synced."
                } catch (e: Exception) { "Error: " + e.message }
                runOnUiThread { status.text = msg; refresh() }
            }.start()
        }

        connect.setOnClickListener { doSync(true) }
        refresh()
        root.addView(TextView(ctx).apply { text = "1. Paste the link and tap Connect.\n2. Tap a design below to add it to the home screen. Repeat for more widgets.\nTap a widget any time to refresh it."; setPadding(0, 0, 0, 24) })
        for (v in listOf<android.view.View>(link, connect, status, designs)) root.addView(v)
        setContentView(ScrollView(ctx).apply { addView(root) })
        if (Store.link(ctx).isNotBlank()) doSync(false)
    }
}
