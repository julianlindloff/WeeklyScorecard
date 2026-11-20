package com.scorecard.weekly.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.scorecard.weekly.ai.ApiKeyStore
import com.scorecard.weekly.ai.CoachChat

@Composable
fun AiKeySection() {
    val keys = ApiKeyStore.get(LocalContext.current)
    val saved = keys.key
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble("✨", MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Claude API key", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (saved.isNullOrBlank()) "Not set: the Coach tab is off" else "Saved: ${ApiKeyStore.masked(saved)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        if (saved.isNullOrBlank() || editing) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; problem = null },
                label = { Text("Paste your API key") },
                placeholder = { Text("sk-ant-…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
                isError = problem != null,
                supportingText = problem?.let { { Text(it) } },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = {
                        val k = input.trim()
                        if (!k.startsWith("sk-ant-")) {
                            problem = "Claude API keys start with \"sk-ant-\"."
                        } else {
                            keys.save(k)
                            input = ""
                            editing = false
                        }
                    },
                    enabled = input.isNotBlank(),
                ) { Text("Save key") }
                if (editing) {
                    OutlinedButton(onClick = { editing = false; input = ""; problem = null }) { Text("Cancel") }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { editing = true }) { Text("Change") }
                OutlinedButton(onClick = { keys.clear(); CoachChat.clear() }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Get a key at console.anthropic.com → API keys, and add some credit under Billing. " +
                "Each question costs a few cents, billed by Anthropic.\n\n" +
                "Privacy: when you ask the coach something, your question and all your recorded weeks " +
                "are sent to Anthropic to create the answer. Nothing is sent unless you use the Coach tab. " +
                "The key stays on this phone and is left out of phone backups.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
