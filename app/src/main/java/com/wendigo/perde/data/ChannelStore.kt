package com.wendigo.perde.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChannelStore(context: Context) {

    data class ImportResult(val added: Int, val skipped: Int, val total: Int)

    private val file = File(context.applicationContext.filesDir, "channels.json")

    @Synchronized
    fun load(): List<Channel> {
        if (!file.exists()) return emptyList()
        return try {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("channels") ?: JSONArray()
            val out = ArrayList<Channel>(array.length())
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val url = item.optString("url")
                if (url.isBlank()) continue
                out.add(
                    Channel(
                        name = item.optString("name", url),
                        url = url,
                        group = item.optString("group").takeIf { it.isNotBlank() },
                        logo = item.optString("logo").takeIf { it.isNotBlank() },
                        source = item.optString("source", "Kaynak")
                    )
                )
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @Synchronized
    fun addSource(name: String, entries: List<M3uParser.Entry>, skipped: Int): ImportResult {
        val current = load()
        val seen = current.mapTo(HashSet()) { it.url }
        val merged = current.toMutableList()
        var added = 0
        for (entry in entries) {
            if (seen.add(entry.url)) {
                merged.add(
                    Channel(
                        name = entry.name,
                        url = entry.url,
                        group = entry.group,
                        logo = entry.logo,
                        source = name
                    )
                )
                added++
            }
        }
        if (added > 0) {
            save(merged)
            rememberSource(name)
        }
        return ImportResult(added, skipped + (entries.size - added), merged.size)
    }

    @Synchronized
    fun sources(): List<String> {
        if (!file.exists()) return emptyList()
        return try {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("sources") ?: JSONArray()
            val out = ArrayList<String>(array.length())
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) out.add(value)
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @Synchronized
    fun clear() {
        if (file.exists()) file.delete()
    }

    private fun rememberSource(name: String) {
        val current = sources().toMutableList()
        if (!current.contains(name)) current.add(name)
        val root = readRoot()
        val array = JSONArray()
        current.forEach { array.put(it) }
        root.put("sources", array)
        writeRoot(root)
    }

    private fun save(channels: List<Channel>) {
        val root = readRoot()
        val array = JSONArray()
        for (channel in channels) {
            array.put(
                JSONObject()
                    .put("name", channel.name)
                    .put("url", channel.url)
                    .put("group", channel.group ?: "")
                    .put("logo", channel.logo ?: "")
                    .put("source", channel.source)
            )
        }
        root.put("channels", array)
        writeRoot(root)
    }

    private fun readRoot(): JSONObject {
        return try {
            if (file.exists()) JSONObject(file.readText()) else JSONObject()
        } catch (_: Throwable) {
            JSONObject()
        }
    }

    private fun writeRoot(root: JSONObject) {
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(root.toString())
        if (!temp.renameTo(file)) {
            file.writeText(root.toString())
            temp.delete()
        }
    }
}
