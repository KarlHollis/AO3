package com.ao3.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ao3.app.viewmodel.WebUiState

@Composable
fun Ao3BottomBar(
    uiState: WebUiState,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onRecentPages: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomActionItem(
                icon = Icons.Filled.Home,
                label = "Home",
                enabled = true,
                onClick = onHome,
                modifier = Modifier.weight(1f),
            )
            BottomActionItem(
                icon = Icons.Filled.Search,
                label = "Search",
                enabled = true,
                onClick = onSearch,
                modifier = Modifier.weight(1f),
            )
            BottomActionItem(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Back",
                enabled = uiState.canGoBack,
                onClick = onBack,
                modifier = Modifier.weight(1f),
            )
            BottomActionItem(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Forward",
                enabled = uiState.canGoForward,
                onClick = onForward,
                modifier = Modifier.weight(1f),
            )
            BottomActionItem(
                icon = Icons.Filled.Refresh,
                label = "Refresh",
                enabled = true,
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                BottomActionItem(
                    icon = Icons.Filled.MoreVert,
                    label = "More",
                    enabled = true,
                    onClick = { menuExpanded = true },
                    modifier = Modifier.fillMaxSize(),
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("最近浏览") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.History, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onRecentPages()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("分享链接") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("复制链接") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onCopyLink()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = LocalContentColor.current
    val tint = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
