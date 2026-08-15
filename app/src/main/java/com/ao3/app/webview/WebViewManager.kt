package com.ao3.app.webview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Message
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ao3.app.util.BrowserActions
import com.ao3.app.viewmodel.DownloadRequest
import com.ao3.app.viewmodel.WebViewModel

class WebViewManager(private val viewModel: WebViewModel) {

    private val activeWebViews = mutableSetOf<WebView>()
    private val activeDialogs = mutableSetOf<Dialog>()

    fun createMainWebView(context: Context): WebView {
        val webView = WebView(context)
        configure(webView)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        webView.webViewClient = Ao3WebViewClient(viewModel)
        webView.webChromeClient = Ao3ChromeClient(viewModel, this)
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            viewModel.onDownloadRequested(DownloadRequest(url, contentDisposition, mimeType))
        }
        activeWebViews += webView
        return webView
    }

    fun onPopupWindowRequested(context: Context, resultMsg: Message): Boolean {
        return try {
            val popup = WebView(context)
            configure(popup)
            showPopupWindow(context, popup, resultMsg)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openInNewWindow(context: Context, url: String): Boolean {
        return try {
            val popup = WebView(context)
            configure(popup)
            showPopupWindow(context, popup, null)
            popup.loadUrl(url)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun flushCookies() {
        CookieManager.getInstance().flush()
    }

    fun destroyAll() {
        activeDialogs.toList().forEach { dialog -> dialog.dismiss() }
        activeDialogs.clear()
        activeWebViews.toList().forEach { webView -> destroyWebView(webView) }
        activeWebViews.clear()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(webView: WebView) {
        val settings = webView.settings

        // JavaScript
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true

        // DOM Storage（localStorage / sessionStorage）
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        // 多窗口：window.open / target=_blank 通过 onCreateWindow 弹窗
        settings.setSupportMultipleWindows(true)

        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = true

        // HTTPS：禁止混合内容，避免 HTTPS 页面降级加载
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = false

        // Cookie：接受 Cookie 与第三方 Cookie，由系统自动持久化以保持登录
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        // 自动适配夜间模式：WebView 内容跟随系统深色模式
        val uiMode = webView.context.resources.configuration.uiMode
        val nightMode = uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                webView.settings.setAlgorithmicDarkeningAllowed(true)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                webView.settings.forceDark = WebSettings.FORCE_DARK_AUTO
            }
            nightMode -> {
                webView.setBackgroundColor(Color.parseColor("#121212"))
            }
        }

        // UserAgent 使用系统默认值，不做自定义覆盖
        val flags = webView.context.applicationInfo.flags
        val debuggable = (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        WebView.setWebContentsDebuggingEnabled(debuggable)

        // 长按链接弹出操作菜单
        webView.setOnLongClickListener { view ->
            handleLongPress(view as WebView)
        }

    }

    private fun handleLongPress(webView: WebView): Boolean {
        val hitTest = webView.hitTestResult
        val url = when (hitTest.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE,
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> hitTest.extra
            else -> null
        } ?: return false
        showLinkContextMenu(webView, url)
        return true
    }

    private fun showLinkContextMenu(webView: WebView, url: String) {
        val options = arrayOf("打开链接", "新窗口打开", "复制链接", "在 Chrome 打开", "分享链接")
        val actions = arrayOf(
            { webView.loadUrl(url) },
            { openInNewWindow(webView.context, url) },
            {
                BrowserActions.copyLink(webView.context, url)
                viewModel.showMessage("链接已复制")
            },
            {
                if (!BrowserActions.openInChrome(webView.context, url)) {
                    viewModel.showMessage("未找到可用的浏览器")
                }
            },
            { BrowserActions.shareLink(webView.context, url) },
        )
        AlertDialog.Builder(webView.context)
            .setTitle("链接操作")
            .setItems(options) { _, which -> actions[which]() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPopupWindow(context: Context, popup: WebView, resultMsg: Message?) {
        val titleView = TextView(context).apply {
            text = "加载中…"
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeButton = Button(context).apply {
            text = "关闭"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 4, 12, 4)
            setBackgroundColor(Color.WHITE)
            addView(titleView)
            addView(closeButton)
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                header,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        popup.webViewClient = PopupWebViewClient()

        var dialog: Dialog? = null
        popup.webChromeClient = PopupChromeClient(
            onTitleChanged = { title -> titleView.text = title },
            onCloseWindow = { dialog?.dismiss() },
            onFileChooser = { callback, params ->
                viewModel.onFileChooserRequested(callback, params)
            },
            onCreateWindow = { ctx, message -> onPopupWindowRequested(ctx, message) },
        )
        popup.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            viewModel.onDownloadRequested(DownloadRequest(url, contentDisposition, mimeType))
        }

        content.addView(
            popup,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        val createdDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(content)
            window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }
        createdDialog.setOnDismissListener {
            destroyWebView(popup)
            activeDialogs.remove(createdDialog)
        }
        dialog = createdDialog

        activeDialogs += createdDialog
        activeWebViews += popup

        if (resultMsg != null) {
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = popup
            resultMsg.sendToTarget()
        }

        createdDialog.show()
        createdDialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    private fun destroyWebView(webView: WebView) {
        if (!activeWebViews.contains(webView)) return
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        activeWebViews.remove(webView)
    }
}
