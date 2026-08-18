package com.ao3.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ao3.app.translation.TranslationEngine
import com.ao3.app.viewmodel.TranslationApiKeys

@Composable
fun TranslationSettingsDialog(
    initialEngine: TranslationEngine,
    initialKeys: TranslationApiKeys,
    onSave: (TranslationEngine, TranslationApiKeys) -> Boolean,
    onDismiss: () -> Unit,
) {
    var engine by remember { mutableStateOf(initialEngine) }
    var deepseekKey by remember { mutableStateOf(initialKeys.deepseekApiKey) }
    var qwenKey by remember { mutableStateOf(initialKeys.qwenApiKey) }
    var youdaoAppKey by remember { mutableStateOf(initialKeys.youdaoAppKey) }
    var youdaoAppSecret by remember { mutableStateOf(initialKeys.youdaoAppSecret) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "翻译引擎设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "选择翻译引擎",
                    style = MaterialTheme.typography.labelLarge,
                )
                TranslationEngine.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { engine = option },
                    ) {
                        RadioButton(
                            selected = engine == option,
                            onClick = { engine = option },
                        )
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                when (engine) {
                    TranslationEngine.ML_KIT -> {
                        Text(
                            text = "端侧离线翻译，无需 Key；免费且可离线，但译文较生硬。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TranslationEngine.DEEPSEEK -> {
                        HintText("DeepSeek API Key（platform.deepseek.com 获取）")
                        OutlinedTextField(
                            value = deepseekKey,
                            onValueChange = { deepseekKey = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    TranslationEngine.QWEN -> {
                        HintText("千问 API Key（阿里云百炼控制台获取）")
                        OutlinedTextField(
                            value = qwenKey,
                            onValueChange = { qwenKey = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    TranslationEngine.YOUDAO -> {
                        HintText("在有道智云控制台创建应用获取 AppKey 与 AppSecret")
                        OutlinedTextField(
                            value = youdaoAppKey,
                            onValueChange = { youdaoAppKey = it },
                            label = { Text("AppKey") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = youdaoAppSecret,
                            onValueChange = { youdaoAppSecret = it },
                            label = { Text("AppSecret") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onSave(engine, TranslationApiKeys(
                            deepseekApiKey = deepseekKey,
                            qwenApiKey = qwenKey,
                            youdaoAppKey = youdaoAppKey,
                            youdaoAppSecret = youdaoAppSecret,
                        ))
                    ) {
                        onDismiss()
                    }
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}
