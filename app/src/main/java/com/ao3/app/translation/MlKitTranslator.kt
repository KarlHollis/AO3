package com.ao3.app.translation

import android.content.Context
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator as MlKitTranslatorClient
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 基于 Google ML Kit 的端侧翻译引擎。
 * 固定翻译为简体中文：先用语言识别判断原文语种（英/日/中等），再自动选择模型。
 * 首次使用某个语言对时会下载约 30MB 的语言模型，之后可离线翻译。
 */
class MlKitTranslator(context: Context) : Translator {

    private val clients = mutableMapOf<String, MlKitTranslatorClient>()
    private val languageIdentifier = LanguageIdentification.getClient()

    override suspend fun translate(
        texts: List<String>,
    ): List<String> = withContext(Dispatchers.IO) {
        val sourceCode = detectSourceLanguage(texts)
        // 原文本身就是中文时无需翻译，直接原样返回
        if (sourceCode == TranslateLanguage.CHINESE) {
            return@withContext texts
        }
        val client = getOrCreateClient(sourceCode)
        ensureModelDownloaded(client)
        texts.map { translateSingle(client, it) }
    }

    private suspend fun detectSourceLanguage(texts: List<String>): String {
        val sample = texts.joinToString(" ").trim().take(MAX_DETECT_CHARS)
        if (sample.isBlank()) return TranslateLanguage.ENGLISH
        val code = runCatching {
            suspendCancellableCoroutine<String> { continuation ->
                languageIdentifier.identifyLanguage(sample)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        }.getOrNull()
        return when {
            code == null || code == "und" -> TranslateLanguage.ENGLISH
            code.startsWith("zh") -> TranslateLanguage.CHINESE
            code == "ja" -> TranslateLanguage.JAPANESE
            else -> TranslateLanguage.ENGLISH
        }
    }

    private fun getOrCreateClient(sourceCode: String): MlKitTranslatorClient {
        return clients.getOrPut(sourceCode) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(TranslateLanguage.CHINESE)
                .build()
            Translation.getClient(options)
        }
    }

    private suspend fun ensureModelDownloaded(client: MlKitTranslatorClient) {
        suspendCancellableCoroutine { continuation ->
            client.downloadModelIfNeeded()
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    private suspend fun translateSingle(client: MlKitTranslatorClient, text: String): String {
        return suspendCancellableCoroutine { continuation ->
            client.translate(text)
                .addOnSuccessListener { translated -> continuation.resume(translated) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    companion object {
        private const val MAX_DETECT_CHARS = 2000
    }
}
