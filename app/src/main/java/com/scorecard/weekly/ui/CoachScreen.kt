package com.scorecard.weekly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.scorecard.weekly.R
import com.scorecard.weekly.ai.ApiKeyStore
import com.scorecard.weekly.ai.ChatMessage
import com.scorecard.weekly.ai.CoachChat
import com.scorecard.weekly.data.ScorecardStore

private val SUGGESTIONS = listOf(
    "How did my last week go?",
    "What should I focus on next week?",
    "Which area has improved the most?",
    "Do you see any patterns in my weeks?",
)

@Composable
fun CoachScreen(store: ScorecardStore, outerPadding: PaddingValues, onOpenSettings: () -> Unit) {
    val keys = ApiKeyStore.get(LocalContext.current)
    val apiKey = keys.key

    Column(
        Modifier
            .fillMaxSize()
            .consumeWindowInsets(outerPadding)
            .imePadding(),
    ) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            ScreenHeader("Coach", overline = "Ask about your weeks") {
                if (CoachChat.messages.isNotEmpty()) {
                    TextButton(onClick = CoachChat::clear) { Text("New chat") }
                }
            }
        }
        if (apiKey.isNullOrBlank()) {
            NoKeyMessage(onOpenSettings)
            return@Column
        }

        val send: (String) -> Unit = { CoachChat.send(it, apiKey, store.weeks.values) }

        Box(Modifier.weight(1f)) {
            if (CoachChat.messages.isEmpty()) {
                Welcome(weekCount = store.weeks.size, onSuggestion = send)
            } else {
                Conversation(onRetry = { CoachChat.retry(apiKey, store.weeks.values) })
            }
        }
        InputBar(onSend = send)
    }
}

@Composable
private fun NoKeyMessage(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBubble("✨", MaterialTheme.colorScheme.primary, size = 72.dp)
        Text(
            "Chat with Claude about your weeks",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
        )
        Text(
            "The coach uses Claude, Anthropic's AI. To turn it on, add your own Claude API key " +
                "from console.anthropic.com in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onOpenSettings) { Text("Add API key") }
    }
}

@Composable
private fun Welcome(weekCount: Int, onSuggestion: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble("✨", MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Your AI coach", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (weekCount == 0) "No weeks recorded yet: score a week first for personal answers."
                        else "Knows all $weekCount recorded week${if (weekCount == 1) "" else "s"}. Ask anything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            "Try asking",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
        SUGGESTIONS.forEach { s ->
            AppCard(onClick = { onSuggestion(s) }, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                Text(s, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Conversation(onRetry: () -> Unit) {
    val listState = rememberLazyListState()
    val messages = CoachChat.messages
    val lastLength = messages.lastOrNull()?.text?.length ?: 0
    // Follow the reply as it streams in.
    LaunchedEffect(messages.size, lastLength, CoachChat.error) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.scrollToItem(count - 1, scrollOffset = Int.MAX_VALUE / 2)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(messages) { _, msg -> Bubble(msg) }
        if (CoachChat.isReplying && messages.lastOrNull()?.fromUser == true) {
            item { Thinking() }
        }
        CoachChat.error?.let { err ->
            item {
                Column {
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    if (CoachChat.canRetry) TextButton(onClick = onRetry) { Text("Try again") }
                }
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (msg.fromUser) 20.dp else 6.dp,
        bottomEnd = if (msg.fromUser) 6.dp else 20.dp,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = shape,
            color = if (msg.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (msg.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            SelectionContainer {
                Text(
                    if (msg.fromUser) AnnotatedString(msg.text) else simpleMarkdown(msg.text),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Thinking() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            "Thinking…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun InputBar(onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val replying = CoachChat.isReplying

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Ask about your weeks…") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        FilledIconButton(
            onClick = {
                if (replying) {
                    CoachChat.stop()
                } else if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                    keyboard?.hide()
                }
            },
            enabled = replying || text.isNotBlank(),
            shape = CircleShape,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(52.dp),
            colors = IconButtonDefaults.filledIconButtonColors(),
        ) {
            Icon(
                painterResource(if (replying) R.drawable.ic_stop else R.drawable.ic_send),
                contentDescription = if (replying) "Stop" else "Send",
            )
        }
    }
}

/** Turns **bold** into bold text; everything else stays as written. */
private fun simpleMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { i, part ->
        // Odd parts sit between a pair of ** markers. An unclosed marker (still streaming) stays plain.
        if (i % 2 == 1 && i < parts.lastIndex) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) }
        } else {
            if (i % 2 == 1) append("**")
            append(part)
        }
    }
}
