package com.ao3.app.webview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ao3.app.translation.TranslationScript
import com.ao3.app.viewmodel.WebViewModel

class Ao3WebViewClient(private val viewModel: WebViewModel) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        viewModel.onPageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        injectDarkModeFallback(view)
        TranslationScript.install(view)
        viewModel.onPageFinished(url)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        viewModel.onHistoryChanged()
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        if (WebUrlPolicy.shouldLoadInWebView(uri)) {
            return false
        }
        return try {
            view?.context?.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (e: ActivityNotFoundException) {
            viewModel.showMessage("无法打开该链接")
            true
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        if (request?.isForMainFrame == true) {
            viewModel.onMainFrameError(
                errorCode = error?.errorCode ?: WebViewClient.ERROR_UNKNOWN,
                description = error?.description?.toString(),
            )
        }
    }

    private fun injectDarkModeFallback(view: WebView?) {
        val webView = view ?: return
        // API 29+ 由 WebView 平台能力处理深色模式，这里只兜底旧版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return
        val uiMode = webView.context.resources.configuration.uiMode
        val nightMode = uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (!nightMode) return
        val script = "(function(){" +
            "var s=document.createElement('style');" +
            "s.id='ao3-dark';" +
            "s.textContent='html{background:#121212 !important;color-scheme:dark;}';" +
            "document.head.appendChild(s);" +
            "})();"
        webView.evaluateJavascript(script, null)
    }
}
