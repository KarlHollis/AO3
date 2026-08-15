package com.ao3.app.translation

interface Translator {

    /**
     * 将一组文本翻译为简体中文（源语言由引擎自动识别）。
     * 返回值与入参一一对应；失败时抛出异常。
     */
    suspend fun translate(texts: List<String>): List<String>
}
