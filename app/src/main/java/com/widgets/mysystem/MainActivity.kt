package com.widgets.mysystem

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
        val add = Button(ctx).apply { text = "Add widget to home screen" }
        val status = TextView(ctx).apply { setPadding(0, 16, 0, 16) }
        val list = TextView(ctx)
        fun refresh() { list.text = "Designs: " + Store.specs(ctx).keys().asSequence().joinToString(", ") }
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
        add.setOnClickListener {
            val mgr = AppWidgetManager.getInstance(ctx)
            if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(ctx, Receiver::class.java), null, null)
            else status.text = "Long-press the home screen, choose Widgets and add this app."
        }
        refresh()
        root.addView(TextView(ctx).apply { text = "1. Paste the link and tap Connect.\n2. Tap Add widget and pick a design.\nTap a widget any time to refresh it."; setPadding(0, 0, 0, 24) })
        for (v in listOf<android.view.View>(link, connect, add, status, list)) root.addView(v)
        setContentView(ScrollView(ctx).apply { addView(root) })
        if (Store.link(ctx).isNotBlank()) doSync(false)
    }
}
