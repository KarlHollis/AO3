package com.ao3.app.translation

enum class TranslationEngine(val displayName: String) {
    ML_KIT("离线翻译（ML Kit）"),
    DEEPSEEK("DeepSeek"),
    QWEN("千问"),
    YOUDAO("有道翻译"),
}
