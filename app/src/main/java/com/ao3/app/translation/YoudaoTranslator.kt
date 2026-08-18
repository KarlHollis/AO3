package com.ao3.app.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ThreadLocalRandom

/**
 * 有道智云文本翻译引擎（v3 签名）。
 * 优先整段（按换行拼接）一次翻译；行数对不上时逐条翻译以保证一一对应。
 */
class YoudaoTranslator(
    private val appKey: String,
    private val appSecret: String,
) : Translator {

    override suspend fun translate(texts: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            if (texts.isEmpty()) return@withContext emptyList()
            val joined = texts.joinToString("\n")
            val translated = translateOnce(joined)
            if (translated.size == texts.size) {
                translated.map { it.trim() }
            } else {
                texts.map { text ->
                    translateOnce(text).firstOrNull()?.trim() ?: text
                }
            }
        }

    private fun translateOnce(text: String): List<String> {
        val salt = ThreadLocalRandom.current().nextInt(100_000, 999_999).toString()
        val curtime = (System.currentTimeMillis() / 1000).toString()
        val sign = sha256Hex(appKey + truncate(text) + salt + curtime + appSecret)
        val params = linkedMapOf(
            "q" to text,
            "from" to "auto",
            "to" to "zh-CHS",
            "appKey" to appKey,
            "salt" to salt,
            "sign" to sign,
            "signType" to "v3",
            "curtime" to curtime,
        )
        val raw = HttpJson.postJson(
            url = ENDPOINT,
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            body = HttpJson.encodeForm(params),
            contentType = "application/x-www-form-urlencoded",
        )
        val json = JSONObject(raw)
        val errorCode = json.optString("errorCode")
        if (errorCode != "0") {
            throw IllegalStateException("有道返回错误码 $errorCode")
        }
        val translation = json.optJSONArray("translation") ?: return emptyList()
        val first = if (translation.length() > 0) translation.optString(0) else ""
        return if (first.isBlank()) emptyList() else first.split("\n")
    }

    private fun truncate(q: String): String =
        if (q.length <= 20) q else q.take(10) + q.length + q.takeLast(10)

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val ENDPOINT = "https://openapi.youdao.com/api"
    }
}
