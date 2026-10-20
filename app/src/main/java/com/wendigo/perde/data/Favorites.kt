package com.wendigo.perde.data

import android.content.Context

class Favorites(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("perde_prefs", Context.MODE_PRIVATE)

    fun all(): Set<String> = prefs.getStringSet(KEY, emptySet()) ?: emptySet()

    fun isFavorite(url: String): Boolean = all().contains(url)

    fun toggle(url: String): Boolean {
        val set = all().toMutableSet()
        val nowFavorite = if (set.contains(url)) {
            set.remove(url)
            false
        } else {
            set.add(url)
            true
        }
        prefs.edit().putStringSet(KEY, set).apply()
        return nowFavorite
    }

    private companion object {
        const val KEY = "favorite_urls"
    }
}
