package com.ao3.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ao3TopBar(
    title: String,
    isTranslating: Boolean,
    isTranslated: Boolean,
    autoTranslate: Boolean,
    onTranslate: () -> Unit,
    onRestoreOriginal: () -> Unit,
    onToggleAutoTranslate: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title.ifBlank { "AO3" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = !isTranslating,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = "翻译",
                        tint = if (isTranslating) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        },
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("翻译") },
                        onClick = {
                            menuExpanded = false
                            onTranslate()
                        },
                    )
                    if (isTranslated) {
                        DropdownMenuItem(
                            text = { Text("恢复原文") },
                            onClick = {
                                menuExpanded = false
                                onRestoreOriginal()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("自动翻译新页面") },
                        trailingIcon = {
                            Text(
                                text = if (autoTranslate) "开" else "关",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleAutoTranslate()
                        },
                    )
                }
            }
        },
    )
}
