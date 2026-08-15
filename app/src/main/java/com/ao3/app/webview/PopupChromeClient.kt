package com.ao3.app.webview

import android.content.Context
import android.net.Uri
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class PopupChromeClient(
    private val onTitleChanged: (String) -> Unit,
    private val onCloseWindow: () -> Unit,
    private val onFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Unit,
    private val onCreateWindow: (Context, Message) -> Boolean = { _, _ -> false },
) : WebChromeClient() {

    override fun onReceivedTitle(view: WebView?, title: String?) {
        title?.takeIf { it.isNotBlank() }?.let(onTitleChanged)
    }

    override fun onCloseWindow(window: WebView?) {
        onCloseWindow()
    }

    override fun onShowFileChooser(
        view: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (filePathCallback != null && fileChooserParams != null) {
            onFileChooser(filePathCallback, fileChooserParams)
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
        return onCreateWindow(context, message)
    }
}
