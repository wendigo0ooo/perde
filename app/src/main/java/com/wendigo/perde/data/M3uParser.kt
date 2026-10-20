package com.wendigo.perde.data

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object M3uParser {

    data class Entry(
        val name: String,
        val url: String,
        val group: String?,
        val logo: String?
    )

    data class Result(
        val entries: List<Entry>,
        val skipped: Int
    )

    private data class Pending(
        val name: String,
        val group: String?,
        val logo: String?
    )

    private val attrRegex = Regex("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")

    fun parse(input: InputStream): Result {
        val found = LinkedHashMap<String, Entry>()
        var pending: Pending? = null
        var skipped = 0

        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
            while (true) {
                val raw = reader.readLine() ?: break
                val line = raw.trim()
                if (line.isEmpty()) continue

                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        pending = parseExtInf(line)
                    }

                    line.startsWith("#") -> Unit

                    else -> {
                        if (!line.contains("://")) {
                            skipped++
                            pending = null
                            continue
                        }
                        val info = pending
                        val name = info?.name?.takeIf { it.isNotBlank() } ?: guessName(line)
                        val entry = Entry(name, line, info?.group, info?.logo)
                        if (found.containsKey(line)) {
                            skipped++
                        } else {
                            found[line] = entry
                        }
                        pending = null
                    }
                }
            }
        }

        return Result(found.values.toList(), skipped)
    }

    private fun parseExtInf(line: String): Pending {
        val comma = line.indexOf(',')
        val meta = if (comma >= 0) line.substring(0, comma) else line
        val display = if (comma >= 0) line.substring(comma + 1).trim() else ""
        val attrs = HashMap<String, String>()
        for (match in attrRegex.findAll(meta)) {
            attrs[match.groupValues[1].lowercase()] = match.groupValues[2].trim()
        }
        val name = attrs["tvg-name"]?.takeIf { it.isNotBlank() } ?: display
        val group = attrs["group-title"]?.takeIf { it.isNotBlank() }
        val logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
        return Pending(name, group, logo)
    }

    private fun guessName(url: String): String {
        val clean = url.substringBefore('?').substringBefore('#').trimEnd('/')
        val tail = clean.substringAfterLast('/')
        return tail.ifBlank { clean }
    }
}
