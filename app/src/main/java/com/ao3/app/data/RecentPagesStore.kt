package com.ao3.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RecentPagesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<RecentPage> {
        val raw = prefs.getString(KEY_RECENT_PAGES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val url = obj.optString("url", "").trim()
                    if (url.isBlank()) continue
                    add(
                        RecentPage(
                            title = obj.optString("title", url),
                            url = url,
                            visitedAt = obj.optLong("visited_at", 0L),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(page: RecentPage): List<RecentPage> {
        val pages = load()
            .filterNot { it.url == page.url }
            .toMutableList()
        pages.add(0, page)
        val trimmed = pages.take(MAX_RECENT_PAGES)
        save(trimmed)
        return trimmed
    }

    fun updateTitle(url: String, title: String): List<RecentPage> {
        val pages = load().map { page ->
            if (page.url == url) page.copy(title = title) else page
        }
        save(pages)
        return pages
    }

    fun clear() {
        prefs.edit().remove(KEY_RECENT_PAGES).apply()
    }

    private fun save(pages: List<RecentPage>) {
        val array = JSONArray()
        pages.forEach { page ->
            array.put(
                JSONObject().apply {
                    put("title", page.title)
                    put("url", page.url)
                    put("visited_at", page.visitedAt)
                }
            )
        }
        prefs.edit().putString(KEY_RECENT_PAGES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "ao3_preferences"
        private const val KEY_RECENT_PAGES = "recent_pages"
        private const val MAX_RECENT_PAGES = 20
    }
}
