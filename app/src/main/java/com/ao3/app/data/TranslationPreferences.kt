package com.ao3.app.data

import android.content.Context

class TranslationPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var autoTranslateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TRANSLATE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "ao3_preferences"
        private const val KEY_AUTO_TRANSLATE = "auto_translate_enabled"
    }
}
