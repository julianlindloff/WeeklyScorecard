package com.scorecard.weekly.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.scorecard.weekly.data.WeekEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

data class ChatMessage(val fromUser: Boolean, val text: String)

/**
 * The conversation with Claude. Lives for as long as the app is open, so switching tabs
 * keeps the chat; it is not saved to storage.
 */
object CoachChat {
    const val MODEL = "claude-opus-5-5"

    val messages = mutableStateListOf<ChatMessage>()
    var isReplying by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var client: AnthropicClient? = null
    private var clientKey: String? = null

    fun send(text: String, apiKey: String, weeks: Collection<WeekEntry>) {
        val question = text.trim()
        if (question.isEmpty() || isReplying) return
        error = null
        messages += ChatMessage(fromUser = true, text = question)
        val history = messages.toList()
        val system = CoachPrompt.systemPrompt(weeks, LocalDate.now())

        isReplying = true
        messages += ChatMessage(fromUser = false, text = "")
        val replyIndex = messages.lastIndex

        job = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    stream(clientFor(apiKey), system, history) { delta ->
                        withContext(Dispatchers.Main) {
                            val current = messages[replyIndex]
                            messages[replyIndex] = current.copy(text = current.text + delta)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Stopped by the user: keep whatever arrived so far.
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                // Drop an empty reply bubble (e.g. after an error) so the chat stays clean.
                if (replyIndex < messages.size && messages[replyIndex].text.isBlank()) {
                    messages.removeAt(replyIndex)
                }
                isReplying = false
                job = null
            }
        }
    }

    /** True when the last question got no answer (after an error), so it can be sent again. */
    val canRetry: Boolean get() = !isReplying && messages.lastOrNull()?.fromUser == true

    fun retry(apiKey: String, weeks: Collection<WeekEntry>) {
        if (!canRetry) return
        val question = messages.removeAt(messages.lastIndex).text
        send(question, apiKey, weeks)
    }

    fun stop() {
        job?.cancel()
    }

    fun clear() {
        stop()
        messages.clear()
        error = null
    }

    private fun clientFor(apiKey: String): AnthropicClient {
        val existing = client
        if (existing != null && clientKey == apiKey) return existing
        existing?.close()
        return AnthropicOkHttpClient.builder().apiKey(apiKey).build().also {
            client = it
            clientKey = apiKey
        }
    }

    private suspend fun stream(
        client: AnthropicClient,
        system: String,
        history: List<ChatMessage>,
        onText: suspend (String) -> Unit,
    ) {
        val builder = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(64000L)
            .system(system)
            // Thinking is always on (adaptive) for this model; effort sets how hard it thinks.
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM).build())
            // If a safety check declines a reply, let the API retry on a suitable fallback model.
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
        history.forEach { msg ->
            if (msg.fromUser) builder.addUserMessage(msg.text) else builder.addAssistantMessage(msg.text)
        }

        var refused = false
        client.messages().createStreaming(builder.build()).use { response ->
            val events = response.stream().iterator()
            while (events.hasNext()) {
                coroutineContext.ensureActive()
                val event = events.next()
                val delta = event.contentBlockDelta().flatMap { it.delta().text() }
                if (delta.isPresent) onText(delta.get().text())
                event.messageDelta().flatMap { it.delta().stopReason() }.ifPresent { reason ->
                    if (reason.toString() == "refusal") refused = true
                }
            }
        }
        if (refused) {
            onText("\n\n(Claude couldn't answer this one. Try rephrasing the question.)")
        }
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is AnthropicServiceException -> when (e.statusCode()) {
            401 -> "Your API key wasn't accepted. Check it in Settings → AI coach."
            403 -> "This API key isn't allowed to use Claude. Check your account at console.anthropic.com."
            429 -> "Too many requests right now. Wait a moment and try again."
            529, 500, 502, 503 -> "Claude is busy right now. Try again in a minute."
            400 -> if (e.message.orEmpty().contains("credit", ignoreCase = true)) {
                "Your Anthropic account is out of credit. Add credit at console.anthropic.com → Billing."
            } else {
                "Claude couldn't process this request: ${e.message}"
            }
            else -> "Something went wrong (${e.statusCode()}): ${e.message}"
        }
        is AnthropicIoException -> "Couldn't reach Claude. Check your internet connection."
        else -> "Something went wrong: ${e.message ?: e.javaClass.simpleName}"
    }
}
