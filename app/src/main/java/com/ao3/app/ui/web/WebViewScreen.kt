package com.ao3.app.ui.web

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ao3.app.ui.components.Ao3BottomBar
import com.ao3.app.ui.components.Ao3TopBar
import com.ao3.app.ui.components.RecentPagesDialog
import com.ao3.app.ui.components.TranslationSettingsDialog
import com.ao3.app.viewmodel.WebViewModel

@Composable
fun WebViewScreen(
    viewModel: WebViewModel,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val recentPages by viewModel.recentPages.collectAsStateWithLifecycle()
    val translationState by viewModel.translationState.collectAsStateWithLifecycle()
    val autoTranslate by viewModel.autoTranslate.collectAsStateWithLifecycle()
    val translationEngine by viewModel.translationEngine.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRecentPages by remember { mutableStateOf(false) }
    var showTranslationSettings by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    BackHandler(enabled = uiState.canGoBack) {
        viewModel.goBack()
    }

    Scaffold(
        topBar = {
            Ao3TopBar(
                title = uiState.title,
                isTranslating = translationState.isTranslating,
                isTranslated = translationState.isTranslated,
                autoTranslate = autoTranslate,
                translationEngine = translationEngine,
                onTranslate = { viewModel.translatePage() },
                onRestoreOriginal = viewModel::restoreOriginal,
                onToggleAutoTranslate = viewModel::toggleAutoTranslate,
                onOpenTranslationSettings = { showTranslationSettings = true },
            )
        },
        bottomBar = {
            Ao3BottomBar(
                uiState = uiState,
                onHome = viewModel::goHome,
                onSearch = viewModel::openSearch,
                onBack = viewModel::goBack,
                onForward = viewModel::goForward,
                onRefresh = viewModel::reload,
                onRecentPages = { showRecentPages = true },
                onShare = onShare,
                onCopyLink = onCopyLink,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AndroidView(
                factory = { context ->
                    viewModel.createMainWebView(context)
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }

            if (uiState.isError) {
                ErrorOverlay(
                    errorMessage = uiState.errorMessage,
                    onRetry = viewModel::reload,
                )
            }
        }
    }

    if (showRecentPages) {
        RecentPagesDialog(
            pages = recentPages,
            onOpen = { page ->
                showRecentPages = false
                viewModel.openPage(page.url)
            },
            onClear = viewModel::clearRecentPages,
            onDismiss = { showRecentPages = false },
        )
    }

    if (showTranslationSettings) {
        TranslationSettingsDialog(
            initialEngine = translationEngine,
            initialKeys = remember { viewModel.currentTranslationApiKeys() },
            onSave = viewModel::saveTranslationSettings,
            onDismiss = { showTranslationSettings = false },
        )
    }
}

@Composable
private fun ErrorOverlay(errorMessage: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "页面加载失败",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = errorMessage ?: "请检查网络连接后重试",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
