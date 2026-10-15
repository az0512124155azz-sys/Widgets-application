package com.widgets.mysystem

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Store {
    fun prefs(c: Context) = c.getSharedPreferences("store", Context.MODE_PRIVATE)

    fun specs(c: Context): JSONObject = try { JSONObject(prefs(c).getString("specs", "{}")!!) } catch (e: Exception) { JSONObject() }

    fun saveSpec(c: Context, spec: JSONObject) {
        val all = specs(c)
        all.put(spec.optString("name", "widget"), spec)
        prefs(c).edit().putString("specs", all.toString()).apply()
    }

    fun assign(c: Context, widgetId: Int, name: String) = prefs(c).edit().putString("w$widgetId", name).apply()

    fun assigned(c: Context, widgetId: Int): JSONObject? {
        val n = prefs(c).getString("w$widgetId", null) ?: return null
        return specs(c).optJSONObject(n)
    }

    fun data(c: Context, name: String): JSONObject = try { JSONObject(prefs(c).getString("d_$name", "{}")!!) } catch (e: Exception) { JSONObject() }

    fun saveData(c: Context, name: String, json: String) = prefs(c).edit().putString("d_$name", json).apply()

    fun fill(t: String, d: JSONObject): String = Regex("\\{([^}]+)\\}").replace(t) { m ->
        var cur: Any? = d
        for (k in m.groupValues[1].split(".")) {
            val c0 = cur
            cur = when (c0) {
                is JSONObject -> c0.opt(k)
                is JSONArray -> k.toIntOrNull()?.let { c0.opt(it) }
                else -> null
            }
        }
        cur?.toString() ?: "--"
    }

    fun http(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 10000
        return c.inputStream.bufferedReader().readText()
    }

    private const val SYSTEM = "You design Android home-screen widgets. Reply with ONLY one JSON object, no markdown, no commentary. " +
        "Schema: {\"name\":\"short_unique_id\",\"bg\":\"#RRGGBB\",\"radius\":0-40,\"padding\":8-24,\"layout\":\"column\" or \"row\"," +
        "\"items\":[{\"type\":\"text\",\"text\":\"may contain {json.path} placeholders filled from the data JSON\",\"size\":10-60,\"color\":\"#RRGGBB\",\"bold\":true/false}," +
        "{\"type\":\"spacer\",\"size\":4-24},{\"type\":\"progress\",\"value\":\"{json.path} giving a number 0..1\",\"color\":\"#RRGGBB\"}]," +
        "\"source\":{\"url\":\"\"}}. Use the keys from the sample JSON for placeholders. Make every design distinct, bold and good looking."

    fun ask(key: String, prompt: String): JSONObject {
        val body = JSONObject().put("model", "claude-sonnet-5-5").put("max_tokens", 1500).put("system", SYSTEM)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("x-api-key", key)
        c.setRequestProperty("anthropic-version", "2023-06-01")
        c.setRequestProperty("content-type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        if (c.responseCode >= 400) throw Exception("API ${c.responseCode}: " + (c.errorStream?.bufferedReader()?.readText() ?: "").take(200))
        val text = JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("content").getJSONObject(0).getString("text")
        return JSONObject(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1))
    }
}
