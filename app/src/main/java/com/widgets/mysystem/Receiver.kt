package com.widgets.mysystem

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GenericWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Sync.schedule(context)
    }
}
