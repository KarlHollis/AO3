package com.ao3.app.translation

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.ao3.app.viewmodel.WebViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * 原生与页面 JS 之间的桥接。
 * 接收 JS 按可见区域分批送来的原文（携带节点索引），
 * 调用 [Translator] 翻译后按索引把译文回填给 JS。
 */
class TranslationJsBridge(
    private val webView: WebView,
    private val translator: Translator,
    private val scope: CoroutineScope,
    private val viewModel: WebViewModel,
) {

    @JavascriptInterface
    fun translateBatch(batchJson: String) {
        val batch = parseBatch(batchJson) ?: return
        val texts = batch.map { it.text }
        scope.launch {
            runCatching { translator.translate(texts) }
                .onSuccess { results ->
                    if (results.size != batch.size) {
                        handleFailure("翻译结果不完整，请重试")
                        return@launch
                    }
                    postToJs(
                        "window.__ao3Translation && " +
                            "window.__ao3Translation.applyResults(${buildResultsJson(batch, results)})",
                    )
                }
                .onFailure { throwable ->
                    handleFailure(throwable.message ?: "翻译失败")
                }
        }
    }

    @JavascriptInterface
    fun onBatchStarted() {
        viewModel.onTranslationBatchStarted()
    }

    @JavascriptInterface
    fun onIdle(translated: Int, total: Int) {
        viewModel.onTranslationIdle(translated, total)
    }

    private fun parseBatch(json: String): List<TranslationItem>? = runCatching {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val index = obj.optInt("i", -1)
                if (index < 0) continue
                add(TranslationItem(index, obj.optString("t")))
            }
        }
    }.getOrNull()

    private fun buildResultsJson(
        batch: List<TranslationItem>,
        results: List<String>,
    ): String {
        val array = JSONArray()
        batch.forEachIndexed { position, item ->
            array.put(
                JSONObject().apply {
                    put("i", item.index)
                    put("t", results[position])
                }
            )
        }
        return array.toString()
    }

    private fun postToJs(script: String) {
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }

    private fun handleFailure(message: String) {
        postToJs("window.__ao3Translation && window.__ao3Translation.restore()")
        viewModel.onTranslationError()
        viewModel.showMessage("翻译失败：$message")
    }

    private data class TranslationItem(
        val index: Int,
        val text: String,
    )
}
