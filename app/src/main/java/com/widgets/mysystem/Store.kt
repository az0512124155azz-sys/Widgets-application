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
        prefs(c).edit().putString("specs", r.getJSONObject("widgets").toString())
            .putString("server_data", (r.optJSONObject("data") ?: JSONObject()).toString()).apply()
    }

    /** Fetch each design's data source. */
    fun refreshData(c: Context) {
        val s = specs(c)
        val sd = prefs(c).getString("server_data", "{}")!!
        for (n in s.keys()) {
            val url = s.getJSONObject(n).optJSONObject("source")?.optString("url") ?: ""
            if (url.isBlank()) saveData(c, n, sd)
            else try { saveData(c, n, http(url)) } catch (e: Exception) { }
        }
    }

    /** Accepts the full connector link (https://host/api/mcp?key=KEY) or just a base URL. */
    fun saveLink(c: Context, link: String) {
        val u = java.net.URI(link.trim())
        val base = u.scheme + "://" + u.host
        val key = (u.rawQuery ?: "").split("&").firstOrNull { it.startsWith("key=") }?.substring(4) ?: ""
        prefs(c).edit().putString("server", base).putString("key", java.net.URLDecoder.decode(key, "UTF-8")).apply()
    }

    fun link(c: Context): String {
        val s = prefs(c).getString("server", "") ?: ""
        return if (s.isBlank()) "" else s + "/api/mcp?key=" + (prefs(c).getString("key", "") ?: "")
    }

    /** A ready-made design so a widget works right after install. */
    fun seedDemo(c: Context) {
        if (specs(c).length() > 0) return
        val demo = JSONObject("""{"name":"demo","bg":"#12141A","radius":24,"padding":14,"layout":"column","items":[
            {"type":"text","text":"MY SYSTEM","size":12,"color":"#8A93A6"},
            {"type":"text","text":"{orders}","size":44,"color":"#FFFFFF","bold":true},
            {"type":"text","text":"tap to refresh","size":12,"color":"#4FD1C5"}]}""")
        prefs(c).edit().putString("specs", JSONObject().put("demo", demo).toString()).apply()
    }
}
