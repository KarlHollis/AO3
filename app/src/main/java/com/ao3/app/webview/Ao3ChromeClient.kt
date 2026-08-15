package com.ao3.app.webview

import android.net.Uri
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.ao3.app.viewmodel.WebViewModel

class Ao3ChromeClient(
    private val viewModel: WebViewModel,
    private val webViewManager: WebViewManager,
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        viewModel.onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        viewModel.onTitleChanged(title)
    }

    override fun onShowFileChooser(
        view: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (filePathCallback != null && fileChooserParams != null) {
            viewModel.onFileChooserRequested(filePathCallback, fileChooserParams)
            return true
        }
        return false
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean {
        val message = resultMsg ?: return false
        val context = view?.context ?: return false
        return webViewManager.onPopupWindowRequested(context, message)
    }
}
