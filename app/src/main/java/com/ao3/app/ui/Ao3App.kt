package com.ao3.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ao3.app.BuildConfig
import com.ao3.app.data.LaunchDialogStore
import com.ao3.app.ui.components.AboutDialog
import com.ao3.app.ui.web.WebViewScreen
import com.ao3.app.util.BrowserActions
import com.ao3.app.viewmodel.WebViewModel

@Composable
fun Ao3App(viewModel: WebViewModel) {
    val context = LocalContext.current
    val launchDialogStore = remember { LaunchDialogStore(context) }
    var showAboutDialog by remember {
        mutableStateOf(launchDialogStore.shouldShow())
    }

    if (showAboutDialog) {
        AboutDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = { showAboutDialog = false },
            onConfirm = { dontShowAgain ->
                launchDialogStore.setSuppressed(dontShowAgain)
                showAboutDialog = false
            },
        )
    }

    WebViewScreen(
        viewModel = viewModel,
        onShare = {
            val url = viewModel.uiState.value.url.ifBlank { WebViewModel.HOME_URL }
            BrowserActions.shareLink(context, url)
        },
        onCopyLink = {
            val url = viewModel.uiState.value.url.ifBlank { WebViewModel.HOME_URL }
            BrowserActions.copyLink(context, url)
            viewModel.showMessage("链接已复制")
        },
    )
}
