package com.ao3.app.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

object BrowserActions {

    private const val CHROME_PACKAGE = "com.android.chrome"

    fun openInChrome(context: Context, url: String): Boolean {
        val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(CHROME_PACKAGE)
            addFlagsIfNeeded(this, context)
        }
        return try {
            context.startActivity(chromeIntent)
            true
        } catch (e: ActivityNotFoundException) {
            openWithSystemBrowser(context, url)
        }
    }

    fun shareLink(context: Context, url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        val chooser = Intent.createChooser(shareIntent, "分享链接")
        addFlagsIfNeeded(chooser, context)
        context.startActivity(chooser)
    }

    fun copyLink(context: Context, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AO3 链接", url))
    }

    private fun openWithSystemBrowser(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlagsIfNeeded(this, context)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun addFlagsIfNeeded(intent: Intent, context: Context) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
