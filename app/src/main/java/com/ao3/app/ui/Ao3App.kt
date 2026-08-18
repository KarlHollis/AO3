package com.ao3.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ao3.app.BuildConfig
import com.ao3.app.data.ChangelogStore
import com.ao3.app.data.LaunchDialogStore
import com.ao3.app.ui.components.AboutDialog
import com.ao3.app.ui.web.WebViewScreen
import com.ao3.app.util.BrowserActions
import com.ao3.app.viewmodel.WebViewModel

@Composable
fun Ao3App(viewModel: WebViewModel) {
    val context = LocalContext.current
    val launchDialogStore = remember { LaunchDialogStore(context) }
    val changelogStore = remember { ChangelogStore(context) }
    var showAboutDialog by remember {
        mutableStateOf(
            changelogStore.shouldShow(BuildConfig.VERSION_NAME) || launchDialogStore.shouldShow(),
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            versionName = BuildConfig.VERSION_NAME,
            changelogItems = CHANGELOG_ITEMS,
            onDismiss = {
                changelogStore.markSeen(BuildConfig.VERSION_NAME)
                showAboutDialog = false
            },
            onConfirm = { dontShowAgain ->
                changelogStore.markSeen(BuildConfig.VERSION_NAME)
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

private val CHANGELOG_ITEMS = listOf(
    "新增翻译引擎设置：DeepSeek / 千问 / 有道翻译（API Key 自填）",
    "保留离线翻译（ML Kit）作为备选",
)
