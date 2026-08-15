package com.ao3.app.webview

import android.net.Uri

object WebUrlPolicy {

    fun shouldLoadInWebView(uri: Uri): Boolean =
        when (uri.scheme?.lowercase()) {
            "http", "https", "blob", "data", "javascript", "about" -> true
            else -> false
        }
}
