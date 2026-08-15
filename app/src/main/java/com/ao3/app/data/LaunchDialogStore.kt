package com.ao3.app.data

import android.content.Context

class LaunchDialogStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShow(): Boolean = !prefs.getBoolean(KEY_SUPPRESS_LAUNCH_DIALOG, false)

    fun setSuppressed(suppressed: Boolean) {
        prefs.edit().putBoolean(KEY_SUPPRESS_LAUNCH_DIALOG, suppressed).apply()
    }

    companion object {
        private const val PREFS_NAME = "ao3_preferences"
        private const val KEY_SUPPRESS_LAUNCH_DIALOG = "suppress_launch_dialog"
    }
}
