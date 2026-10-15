package com.widgets.mysystem

import android.app.Activity
import android.os.Bundle
import android.widget.*
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.runBlocking

class MainActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val ctx = this
        Sync.schedule(ctx)
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 96, 40, 40) }
        val server = EditText(ctx).apply { hint = "Server URL (https://xxx.vercel.app)"; setText(Store.prefs(ctx).getString("server", "")) }
        val key = EditText(ctx).apply { hint = "Key (MCP_KEY)"; setText(Store.prefs(ctx).getString("key", "")) }
        val btn = Button(ctx).apply { text = "Sync widgets" }
        val status = TextView(ctx)
        val list = TextView(ctx)
        fun refresh() { list.text = "Designs: " + Store.specs(ctx).keys().asSequence().joinToString(", ") }
        refresh()
        btn.setOnClickListener {
            Store.prefs(ctx).edit().putString("server", server.text.toString()).putString("key", key.text.toString()).apply()
            status.text = "Syncing..."
            val app = applicationContext
            Thread {
                val msg = try {
                    Store.sync(app)
                    Store.refreshData(app)
                    runBlocking { GenericWidget().updateAll(app) }
                    "Synced. Add a widget from the home screen and pick a design."
                } catch (e: Exception) { "Error: " + e.message }
                runOnUiThread { status.text = msg; refresh() }
            }.start()
        }
        root.addView(TextView(ctx).apply { text = "Create designs by chatting with Claude (connector), then sync here."; setPadding(0, 0, 0, 24) })
        for (v in listOf<android.view.View>(server, key, btn, status, list)) root.addView(v)
        setContentView(ScrollView(ctx).apply { addView(root) })
    }
}
