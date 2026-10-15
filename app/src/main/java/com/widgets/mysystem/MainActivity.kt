package com.widgets.mysystem

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class MainActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val ctx = this
        Sync.schedule(ctx)
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 96, 40, 40) }
        fun et(h: String) = EditText(ctx).apply { hint = h }
        val key = et("Anthropic API key").apply {
            setText(Store.prefs(ctx).getString("key", ""))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val desc = et("Describe the widget (style, what to show)").apply { minLines = 3; gravity = Gravity.TOP }
        val url = et("Data URL returning JSON (optional)")
        val btn = Button(ctx).apply { text = "Create widget with AI" }
        val status = TextView(ctx)
        val list = TextView(ctx)
        fun refresh() { list.text = "Saved designs: " + Store.specs(ctx).keys().asSequence().joinToString(", ") }
        refresh()
        btn.setOnClickListener {
            val k = key.text.toString().trim()
            val u = url.text.toString().trim()
            val d = desc.text.toString().trim()
            Store.prefs(ctx).edit().putString("key", k).apply()
            status.text = "Generating..."
            val app = applicationContext
            Thread {
                val msg = try {
                    var raw = ""
                    if (u.isNotBlank()) raw = Store.http(u)
                    val spec = Store.ask(k, "Widget request: " + d + "\nData URL: " + u + "\nSample JSON: " + raw.take(1500))
                    if (u.isNotBlank()) spec.put("source", JSONObject().put("url", u))
                    Store.saveSpec(app, spec)
                    if (raw.isNotBlank()) Store.saveData(app, spec.optString("name"), raw)
                    runBlocking { GenericWidget().updateAll(app) }
                    "Created: " + spec.optString("name") + ". Long-press the home screen, add the widget and pick it."
                } catch (e: Exception) { "Error: " + e.message }
                runOnUiThread { status.text = msg; refresh() }
            }.start()
        }
        for (v in listOf<android.view.View>(key, desc, url, btn, status, list)) root.addView(v)
        setContentView(ScrollView(ctx).apply { addView(root) })
    }
}
