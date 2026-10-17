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
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

fun col(s: String): Color = try { Color(android.graphics.Color.parseColor(s)) } catch (e: Exception) { Color.White }

class GenericWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wid = GlanceAppWidgetManager(context).getAppWidgetId(id)
        var spec = Store.assigned(context, wid)
        val all = Store.specs(context)
        val first = all.keys().asSequence().firstOrNull { it != "demo" }
        if (first != null && (spec == null || spec?.optString("name") == "demo")) {
            Store.assign(context, wid, first)
            spec = all.optJSONObject(first)
        } else if (spec == null) {
            val any = all.keys().asSequence().firstOrNull()
            if (any != null) { Store.assign(context, wid, any); spec = all.optJSONObject(any) }
        }
        val finalSpec = spec
        val data = if (finalSpec != null) Store.data(context, finalSpec.optString("name")) else JSONObject()
        provideContent { Render(finalSpec, data) }
    }
}

@Composable
fun Render(spec: JSONObject?, d: JSONObject) {
    if (spec == null) {
        Box(
            GlanceModifier.fillMaxSize().background(Color(0xFF12141A)).cornerRadius(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Open the MySystem app and tap Connect", style = TextStyle(color = ColorProvider(Color.White)))
        }
        return
    }
    val items = spec.optJSONArray("items") ?: JSONArray()
    val mod = GlanceModifier.fillMaxSize().background(col(spec.optString("bg", "#12141A")))
        .cornerRadius(spec.optInt("radius", 24).dp).padding(spec.optInt("padding", 14).dp)
        .clickable(actionRunCallback<RefreshAction>())
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

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.IO) {
            try { Store.sync(context) } catch (e: Exception) { }
            Store.refreshData(context)
        }
        GenericWidget().updateAll(context)
    }
}
