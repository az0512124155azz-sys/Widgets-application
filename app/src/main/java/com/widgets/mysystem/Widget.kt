package com.widgets.mysystem

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val p = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
        val value = p.getString("value", "--") ?: "--"
        val subtitle = p.getString("subtitle", "waiting for data") ?: ""
        provideContent { Content(value, subtitle) }
    }

    @Composable
    private fun Content(value: String, subtitle: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color(0xFF12141A)).cornerRadius(24.dp).padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text("המערכת שלי", style = TextStyle(color = ColorProvider(Color(0xFF8A93A6)), fontSize = 12.sp))
            Text(value, style = TextStyle(color = ColorProvider(Color.White), fontSize = 44.sp, fontWeight = FontWeight.Bold))
            Text(subtitle, style = TextStyle(color = ColorProvider(Color(0xFF4FD1C5)), fontSize = 13.sp))
        }
    }
}
