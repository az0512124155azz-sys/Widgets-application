package com.widgets.mysystem

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.runBlocking

class ConfigActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid))
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 96, 40, 40) }
        root.addView(TextView(this).apply { text = "Choose a design"; textSize = 20f })
        val names = Store.specs(this).keys().asSequence().toList()
        if (names.isEmpty()) root.addView(TextView(this).apply { text = "No designs yet. Create one in the app first." })
        for (n in names) root.addView(Button(this).apply {
            text = n
            setOnClickListener {
                Store.assign(this@ConfigActivity, wid, n)
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid))
                val app = applicationContext
                Thread { runBlocking { GenericWidget().updateAll(app) } }.start()
                finish()
            }
        })
        setContentView(root)
    }
}
