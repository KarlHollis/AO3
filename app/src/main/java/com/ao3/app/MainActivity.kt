package com.ao3.app

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ao3.app.ui.Ao3App
import com.ao3.app.ui.theme.Ao3Theme
import com.ao3.app.upload.FileUploadManager
import com.ao3.app.viewmodel.DownloadRequest
import com.ao3.app.viewmodel.WebViewModel
import com.ao3.app.webview.DownloadManagerHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val webViewModel: WebViewModel by lazy {
        ViewModelProvider(this)[WebViewModel::class.java]
    }

    private var pendingDownload: DownloadRequest? = null
    private val fileUploadManager = FileUploadManager(this)

    private val downloadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val request = pendingDownload
            pendingDownload = null
            when {
                granted && request != null -> webViewModel.confirmDownload(request)
                request != null -> webViewModel.dismissDownloadRequest()
            }
        }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L) return
            val message = DownloadManagerHelper.handleDownloadComplete(
                this@MainActivity.applicationContext,
                downloadId,
            ) ?: return
            webViewModel.showMessage(message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Ao3Theme {
                Ao3App(viewModel = webViewModel)
            }
        }
        observeWebViewRequests()
        ContextCompat.registerReceiver(
            this,
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onPause() {
        super.onPause()
        webViewModel.onAppPause()
    }

    override fun onResume() {
        super.onResume()
        webViewModel.onAppResume()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        webViewModel.onTrimMemory(level)
    }

    override fun onDestroy() {
        unregisterReceiver(downloadCompleteReceiver)
        super.onDestroy()
    }

    private fun observeWebViewRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    webViewModel.downloadRequest.collect { request ->
                        if (request == null) return@collect
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            webViewModel.confirmDownload(request)
                        } else {
                            pendingDownload = request
                            downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
                launch {
                    webViewModel.fileChooserRequest.collect { request ->
                        if (request == null) return@collect
                        fileUploadManager.handleRequest(request)
                        webViewModel.clearFileChooser()
                    }
                }
            }
        }
    }
}
