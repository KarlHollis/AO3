package com.ao3.app.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 兼容 OpenAI Chat Completions 格式的大模型翻译引擎（DeepSeek / 千问）。
 * 一次请求翻译整个批次，要求模型返回与原数组等长、顺序一致的 JSON 字符串数组。
 */
class LlmBatchTranslator(
    private val endpoint: String,
    private val apiKey: String,
    private val model: String,
) : Translator {

    override suspend fun translate(texts: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            if (texts.isEmpty()) return@withContext emptyList()
            val input = JSONArray(texts).toString()
            val userPrompt = "请把下面 JSON 数组中的每一项翻译成简体中文，" +
                "输出一个同样长度、顺序一致的 JSON 字符串数组，直接输出数组本身，不要包含任何解释或额外文字：\n$input"
            val first = runCatching {
                parseJsonArray(requestCompletion(userPrompt))
            }.getOrNull()
            if (first != null && first.size == texts.size) {
                return@withContext first
            }
            val retry = runCatching {
                parseJsonArray(
                    requestCompletion("$userPrompt\n再次强调：只输出 JSON 数组，不要解释。"),
                )
            }.getOrNull()
            if (retry != null && retry.size == texts.size) {
                return@withContext retry
            }
            val got = retry?.size ?: first?.size ?: 0
            throw IllegalStateException("模型返回结果数量不匹配（$got vs ${texts.size}）")
        }

    private fun requestCompletion(userPrompt: String): String {
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.3)
            put("stream", false)
            put(
                "messages",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        }
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                    )
                }
            )
        }
        val raw = HttpJson.postJson(
            url = endpoint,
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json",
            ),
            body = body.toString(),
        )
        val json = JSONObject(raw)
        json.optJSONObject("error")?.let { error ->
            throw IllegalStateException(error.optString("message").ifBlank { "接口返回错误" })
        }
        val choices = json.optJSONArray("choices")
            ?: throw IllegalStateException("接口返回异常")
        if (choices.length() == 0) throw IllegalStateException("接口返回为空")
        val content = choices
            .getJSONObject(0)
            .optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        if (content.isBlank()) throw IllegalStateException("接口返回为空")
        return content
    }

    private fun parseJsonArray(content: String): List<String> {
        val cleaned = content
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start < 0 || end <= start) throw IllegalStateException("无法解析模型返回")
        val array = JSONArray(cleaned.substring(start, end + 1))
        return buildList {
            for (i in 0 until array.length()) {
                add(array.optString(i))
            }
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = "你是一位专业的中文文学翻译。" +
            "将用户提供的文本翻译成简体中文，要求自然、通顺，符合中文小说阅读习惯，不要逐字直译。" +
            "只输出结果，不要任何解释或额外内容。"
    }
}
