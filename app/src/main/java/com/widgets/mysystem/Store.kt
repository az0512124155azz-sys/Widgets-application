package com.widgets.mysystem

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Store {
    fun prefs(c: Context) = c.getSharedPreferences("store", Context.MODE_PRIVATE)

    fun specs(c: Context): JSONObject = try { JSONObject(prefs(c).getString("specs", "{}")!!) } catch (e: Exception) { JSONObject() }

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

    /** Pull widget designs from the MCP server (throws on failure). */
    fun sync(c: Context) {
        val base = prefs(c).getString("server", "")!!.trim().trimEnd('/')
        val key = prefs(c).getString("key", "")!!.trim()
        if (base.isBlank()) throw Exception("Enter the server URL")
        val r = JSONObject(http(base + "/api/widgets?key=" + URLEncoder.encode(key, "UTF-8")))
        prefs(c).edit().putString("specs", r.getJSONObject("widgets").toString()).apply()
    }

    /** Fetch each design's data source. */
    fun refreshData(c: Context) {
        val s = specs(c)
        for (n in s.keys()) {
            val url = s.getJSONObject(n).optJSONObject("source")?.optString("url") ?: ""
            if (url.isNotBlank()) try { saveData(c, n, http(url)) } catch (e: Exception) { }
        }
    }
}
