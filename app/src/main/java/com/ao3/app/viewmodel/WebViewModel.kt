package com.ao3.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ao3.app.data.RecentPage
import com.ao3.app.data.RecentPagesStore
import com.ao3.app.data.TranslationPreferences
import com.ao3.app.translation.LlmBatchTranslator
import com.ao3.app.translation.MlKitTranslator
import com.ao3.app.translation.TranslationJsBridge
import com.ao3.app.translation.TranslationEngine
import com.ao3.app.translation.TranslationScript
import com.ao3.app.translation.Translator
import com.ao3.app.translation.YoudaoTranslator
import com.ao3.app.webview.DownloadManagerHelper
import com.ao3.app.webview.WebViewManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WebUiState(
    val title: String = "AO3",
    val url: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
)

data class DownloadRequest(
    val url: String,
    val contentDisposition: String?,
    val mimeType: String?,
)

data class FileChooserRequest(
    val callback: ValueCallback<Array<Uri>>,
    val params: WebChromeClient.FileChooserParams,
)

data class TranslationUiState(
    val isTranslating: Boolean = false,
    val isTranslated: Boolean = false,
    val isFullyTranslated: Boolean = false,
)

data class TranslationApiKeys(
    val deepseekApiKey: String = "",
    val qwenApiKey: String = "",
    val youdaoAppKey: String = "",
    val youdaoAppSecret: String = "",
)

class WebViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val HOME_URL = "https://archiveofourown.org"
        const val SEARCH_URL = "https://archiveofourown.org/works/search"
        private const val DEEPSEEK_ENDPOINT = "https://api.deepseek.com/chat/completions"
        private const val QWEN_ENDPOINT =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    }

    private val _uiState = MutableStateFlow(WebUiState())
    val uiState: StateFlow<WebUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _downloadRequest = MutableStateFlow<DownloadRequest?>(null)
    val downloadRequest: StateFlow<DownloadRequest?> = _downloadRequest.asStateFlow()

    private val _fileChooserRequest = MutableStateFlow<FileChooserRequest?>(null)
    val fileChooserRequest: StateFlow<FileChooserRequest?> = _fileChooserRequest.asStateFlow()

    private val recentPagesStore = RecentPagesStore(getApplication())
    private val _recentPages = MutableStateFlow(recentPagesStore.load())
    val recentPages: StateFlow<List<RecentPage>> = _recentPages.asStateFlow()

    private val _translationState = MutableStateFlow(TranslationUiState())
    val translationState: StateFlow<TranslationUiState> = _translationState.asStateFlow()

    private val _autoTranslate = MutableStateFlow(false)
    val autoTranslate: StateFlow<Boolean> = _autoTranslate.asStateFlow()

    private val translationPreferences = TranslationPreferences(getApplication())
    private var translator: Translator = buildTranslator(translationPreferences)

    private val _translationEngine = MutableStateFlow(translationPreferences.translationEngine)
    val translationEngine: StateFlow<TranslationEngine> = _translationEngine.asStateFlow()

    private var webView: WebView? = null
    private var translationJsBridge: TranslationJsBridge? = null
    private var translationStartedUrl: String? = null
    private val webViewManager = WebViewManager(this)

    fun createMainWebView(context: Context): WebView {
        _autoTranslate.value = translationPreferences.autoTranslateEnabled
        val webView = webViewManager.createMainWebView(context)
        val bridge = TranslationJsBridge(webView, translator, viewModelScope, this)
        translationJsBridge = bridge
        webView.addJavascriptInterface(bridge, TranslationScript.BRIDGE_NAME)
        this.webView = webView
        if (webView.url.isNullOrBlank()) {
            webView.loadUrl(_uiState.value.url.ifBlank { HOME_URL })
        }
        return webView
    }

    fun onAppPause() {
        webViewManager.flushCookies()
        webViewManager.pauseAll()
    }

    fun onAppResume() {
        webViewManager.resumeAll()
    }

    fun onTrimMemory(level: Int) {
        webViewManager.onTrimMemory(level)
    }

    fun onPageStarted(url: String?) {
        _translationState.value = TranslationUiState()
        translationStartedUrl = null
        _uiState.update {
            it.copy(
                url = url ?: it.url,
                progress = 10,
                isLoading = true,
                isError = false,
                errorMessage = null,
            )
        }
    }

    fun onPageFinished(url: String?) {
        val current = webView
        val finalUrl = url ?: _uiState.value.url
        _uiState.update {
            it.copy(
                url = finalUrl,
                progress = 100,
                isLoading = false,
                canGoBack = current?.canGoBack() == true,
                canGoForward = current?.canGoForward() == true,
            )
        }
        if (finalUrl.startsWith("https://") || finalUrl.startsWith("http://")) {
            recordRecentPage(_uiState.value.title.ifBlank { finalUrl }, finalUrl)
        }
        if (_autoTranslate.value && !_uiState.value.isError) {
            translatePage(showHint = false)
        }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.update { it.copy(progress = progress) }
    }

    fun onTitleChanged(title: String?) {
        if (!title.isNullOrBlank()) {
            _uiState.update { it.copy(title = title) }
            val currentUrl = _uiState.value.url
            if (currentUrl.startsWith("https://") || currentUrl.startsWith("http://")) {
                _recentPages.value = recentPagesStore.updateTitle(currentUrl, title)
            }
        }
    }

    fun onHistoryChanged() {
        val current = webView
        _uiState.update {
            it.copy(
                canGoBack = current?.canGoBack() == true,
                canGoForward = current?.canGoForward() == true,
            )
        }
    }

    fun onMainFrameError(errorCode: Int, description: String?) {
        val message = when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP -> "无法解析服务器地址，请检查网络连接"
            WebViewClient.ERROR_CONNECT -> "无法连接到服务器，请检查网络连接"
            WebViewClient.ERROR_TIMEOUT -> "连接超时，请稍后重试"
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "安全连接失败，请检查网络"
            WebViewClient.ERROR_BAD_URL -> "链接地址无效"
            WebViewClient.ERROR_TOO_MANY_REQUESTS -> "请求过于频繁，请稍后重试"
            else -> description?.takeIf { it.isNotBlank() } ?: "页面加载失败，请检查网络连接"
        }
        _uiState.update { it.copy(isLoading = false, isError = true, errorMessage = message) }
        showMessage(message)
    }

    fun goBack() {
        val current = webView ?: return
        if (current.canGoBack()) current.goBack()
    }

    fun goForward() {
        val current = webView ?: return
        if (current.canGoForward()) current.goForward()
    }

    fun reload() {
        webView?.reload()
    }

    fun goHome() {
        webView?.loadUrl(HOME_URL)
    }

    fun openSearch() {
        webView?.loadUrl(SEARCH_URL)
    }

    fun openPage(url: String) {
        webView?.loadUrl(url)
    }

    fun recordRecentPage(title: String, url: String) {
        val page = RecentPage(title = title, url = url, visitedAt = System.currentTimeMillis())
        _recentPages.value = recentPagesStore.add(page)
    }

    fun clearRecentPages() {
        recentPagesStore.clear()
        _recentPages.value = emptyList()
    }

    fun translatePage(showHint: Boolean = true) {
        val current = webView
        if (current == null) {
            showMessage("页面尚未加载完成，请稍后再试")
            return
        }
        if (_translationState.value.isTranslating) {
            showMessage("正在翻译中，请稍候")
            return
        }
        _translationState.update {
            it.copy(
                isTranslating = true,
                isTranslated = false,
                isFullyTranslated = false,
            )
        }
        translationStartedUrl = _uiState.value.url
        if (showHint) {
            showMessage("正在翻译为简体中文（随滚动逐段翻译，首次使用需下载翻译模型）…")
        }
        TranslationScript.start(current) { started ->
            if (!started) {
                translationStartedUrl = null
                _translationState.value = TranslationUiState()
                showMessage("页面尚未加载完成，请稍后再试")
            }
        }
    }

    fun restoreOriginal() {
        val current = webView ?: return
        if (_translationState.value.isTranslating) return
        TranslationScript.restore(current)
        translationStartedUrl = null
        _translationState.value = TranslationUiState()
    }

    fun onTranslationBatchStarted() {
        _translationState.update { it.copy(isTranslating = true) }
    }

    fun onTranslationIdle(translated: Int, total: Int) {
        if (translationStartedUrl != null && translationStartedUrl != _uiState.value.url) {
            translationStartedUrl = null
            _translationState.value = TranslationUiState()
            return
        }
        translationStartedUrl = null
        val fullyTranslated = total > 0 && translated >= total
        val wasFullyTranslated = _translationState.value.isFullyTranslated
        _translationState.update {
            it.copy(
                isTranslating = false,
                isTranslated = translated > 0 || it.isTranslated,
                isFullyTranslated = fullyTranslated,
            )
        }
        if (fullyTranslated && !wasFullyTranslated) {
            showMessage("翻译完成")
        }
    }

    fun setAutoTranslate(enabled: Boolean) {
        translationPreferences.autoTranslateEnabled = enabled
        _autoTranslate.value = enabled
        if (enabled) {
            translatePage(showHint = false)
        }
    }

    fun toggleAutoTranslate() {
        setAutoTranslate(!_autoTranslate.value)
    }

    fun currentTranslationApiKeys(): TranslationApiKeys = TranslationApiKeys(
        deepseekApiKey = translationPreferences.deepseekApiKey,
        qwenApiKey = translationPreferences.qwenApiKey,
        youdaoAppKey = translationPreferences.youdaoAppKey,
        youdaoAppSecret = translationPreferences.youdaoAppSecret,
    )

    fun currentDeepseekModel(): String = translationPreferences.deepseekModel

    fun currentQwenModel(): String = translationPreferences.qwenModel

    fun saveTranslationSettings(
        engine: TranslationEngine,
        keys: TranslationApiKeys,
        deepseekModel: String,
        qwenModel: String,
    ): Boolean {
        val requiredKey = when (engine) {
            TranslationEngine.ML_KIT -> null
            TranslationEngine.DEEPSEEK -> keys.deepseekApiKey
            TranslationEngine.QWEN -> keys.qwenApiKey
            TranslationEngine.YOUDAO -> keys.youdaoAppKey
        }
        if (engine != TranslationEngine.ML_KIT && requiredKey.isNullOrBlank()) {
            showMessage("请先填写对应引擎的 API Key")
            return false
        }
        if (engine == TranslationEngine.YOUDAO && keys.youdaoAppSecret.isBlank()) {
            showMessage("请填写有道 AppSecret")
            return false
        }
        translationPreferences.translationEngine = engine
        translationPreferences.deepseekApiKey = keys.deepseekApiKey.trim()
        translationPreferences.qwenApiKey = keys.qwenApiKey.trim()
        translationPreferences.youdaoAppKey = keys.youdaoAppKey.trim()
        translationPreferences.youdaoAppSecret = keys.youdaoAppSecret.trim()
        translationPreferences.deepseekModel = deepseekModel.ifBlank {
            TranslationPreferences.DEEPSEEK_DEFAULT_MODEL
        }
        translationPreferences.qwenModel = qwenModel.ifBlank {
            TranslationPreferences.QWEN_DEFAULT_MODEL
        }
        _translationEngine.value = engine
        translator = buildTranslator(translationPreferences)
        translationJsBridge?.updateTranslator(translator)
        showMessage("翻译引擎已切换：${engine.displayName}")
        return true
    }

    fun onTranslationError() {
        translationStartedUrl = null
        _translationState.value = TranslationUiState()
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun onDownloadRequested(request: DownloadRequest) {
        _downloadRequest.value = request
    }

    fun confirmDownload(request: DownloadRequest) {
        _downloadRequest.value = null
        try {
            DownloadManagerHelper.enqueue(getApplication(), request)
            showMessage("开始下载：${DownloadManagerHelper.guessFileName(request)}")
        } catch (e: Exception) {
            showMessage("下载失败：${DownloadManagerHelper.guessFileName(request)}")
        }
    }

    fun dismissDownloadRequest() {
        _downloadRequest.value = null
    }

    fun onFileChooserRequested(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams,
    ) {
        _fileChooserRequest.value = FileChooserRequest(callback, params)
    }

    fun clearFileChooser() {
        _fileChooserRequest.value = null
    }

    override fun onCleared() {
        webViewManager.destroyAll()
        webView = null
    }

    private fun buildTranslator(preferences: TranslationPreferences): Translator =
        when (preferences.translationEngine) {
            TranslationEngine.ML_KIT -> MlKitTranslator(getApplication())
            TranslationEngine.DEEPSEEK -> LlmBatchTranslator(
                endpoint = DEEPSEEK_ENDPOINT,
                apiKey = preferences.deepseekApiKey,
                model = preferences.deepseekModel,
            )
            TranslationEngine.QWEN -> LlmBatchTranslator(
                endpoint = QWEN_ENDPOINT,
                apiKey = preferences.qwenApiKey,
                model = preferences.qwenModel,
            )
            TranslationEngine.YOUDAO -> YoudaoTranslator(
                appKey = preferences.youdaoAppKey,
                appSecret = preferences.youdaoAppSecret,
            )
        }
}
