package com.widgets.mysystem

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.json.JSONArray
import org.json.JSONObject

fun col(s: String): Color = try { Color(android.graphics.Color.parseColor(s)) } catch (e: Exception) { Color.White }

class GenericWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wid = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val spec = Store.assigned(context, wid)
        val data = if (spec != null) Store.data(context, spec.optString("name")) else JSONObject()
        provideContent { Render(spec, data) }
    }
}

@Composable
fun Render(spec: JSONObject?, d: JSONObject) {
    if (spec == null) {
        Box(GlanceModifier.fillMaxSize().background(Color(0xFF12141A)), contentAlignment = Alignment.Center) {
            Text("Open the app", style = TextStyle(color = ColorProvider(Color.White)))
        }
        return
    }
    val items = spec.optJSONArray("items") ?: JSONArray()
    val mod = GlanceModifier.fillMaxSize().background(col(spec.optString("bg", "#12141A")))
        .cornerRadius(spec.optInt("radius", 24).dp).padding(spec.optInt("padding", 14).dp)
    if (spec.optString("layout", "column") == "row") {
        Row(modifier = mod, verticalAlignment = Alignment.Vertical.CenterVertically, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            for (i in 0 until items.length()) Item(items.getJSONObject(i), d)
        }
    } else {
        Column(modifier = mod, verticalAlignment = Alignment.Vertical.CenterVertically, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            for (i in 0 until items.length()) Item(items.getJSONObject(i), d)
        }
    }
}

@Composable
fun Item(o: JSONObject, d: JSONObject) {
    when (o.optString("type", "text")) {
        "spacer" -> Spacer(GlanceModifier.height(o.optInt("size", 8).dp).width(o.optInt("size", 8).dp))
        "progress" -> LinearProgressIndicator(
            progress = Store.fill(o.optString("value", "0"), d).toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
            modifier = GlanceModifier.fillMaxWidth(),
            color = ColorProvider(col(o.optString("color", "#4FD1C5"))),
            backgroundColor = ColorProvider(Color(0x33FFFFFF))
        )
        else -> Text(
            Store.fill(o.optString("text"), d),
            style = TextStyle(
                color = ColorProvider(col(o.optString("color", "#FFFFFF"))),
                fontSize = o.optInt("size", 14).sp,
                fontWeight = if (o.optBoolean("bold")) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
