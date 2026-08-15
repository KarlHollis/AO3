package com.ao3.app.data

import android.content.Context

class ChangelogStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShow(currentVersion: String): Boolean =
        prefs.getString(KEY_LAST_SEEN_VERSION, null) != currentVersion

    fun markSeen(currentVersion: String) {
        prefs.edit().putString(KEY_LAST_SEEN_VERSION, currentVersion).apply()
    }

    companion object {
        private const val PREFS_NAME = "ao3_preferences"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_changelog_version"
    }
}
