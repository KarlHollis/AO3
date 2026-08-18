package com.ao3.app.data

import android.content.Context
import com.ao3.app.translation.TranslationEngine

class TranslationPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var autoTranslateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TRANSLATE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()
        }

    var translationEngine: TranslationEngine
        get() {
            val raw = prefs.getString(KEY_TRANSLATION_ENGINE, null)
            return TranslationEngine.entries.firstOrNull { it.name == raw }
                ?: TranslationEngine.ML_KIT
        }
        set(value) {
            prefs.edit().putString(KEY_TRANSLATION_ENGINE, value.name).apply()
        }

    var deepseekApiKey: String
        get() = prefs.getString(KEY_DEEPSEEK_API_KEY, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_DEEPSEEK_API_KEY, value).apply()
        }

    var qwenApiKey: String
        get() = prefs.getString(KEY_QWEN_API_KEY, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_QWEN_API_KEY, value).apply()
        }

    var youdaoAppKey: String
        get() = prefs.getString(KEY_YOUDAO_APP_KEY, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_YOUDAO_APP_KEY, value).apply()
        }

    var youdaoAppSecret: String
        get() = prefs.getString(KEY_YOUDAO_APP_SECRET, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_YOUDAO_APP_SECRET, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "ao3_preferences"
        private const val KEY_AUTO_TRANSLATE = "auto_translate_enabled"
        private const val KEY_TRANSLATION_ENGINE = "translation_engine"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_QWEN_API_KEY = "qwen_api_key"
        private const val KEY_YOUDAO_APP_KEY = "youdao_app_key"
        private const val KEY_YOUDAO_APP_SECRET = "youdao_app_secret"
    }
}
