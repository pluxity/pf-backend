package com.pluxity.safers.llm

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.safers.llm.dto.Message
import com.pluxity.safers.llm.dto.OllamaChatRequest
import com.pluxity.safers.llm.dto.OllamaChatResponse
import com.pluxity.safers.llm.dto.OllamaOptions
import com.pluxity.safers.llm.dto.OpenRouterChatRequest
import com.pluxity.safers.llm.dto.OpenRouterChatResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.Retry
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}

@Component
class LlmClient(
    webClientFactory: WebClientFactory,
    private val llmProperties: LlmProperties,
) {
    private val ollamaClient: WebClient? =
        llmProperties.ollama.takeIf { it.isEnabled }?.let {
            webClientFactory.createClient(
                baseUrl = it.baseUrl,
                responseTimeoutMs = llmProperties.timeoutMs,
                readTimeoutMs = llmProperties.timeoutMs,
            )
        }

    private val openRouterClient: WebClient? =
        llmProperties.openrouter.takeIf { it.isEnabled }?.let {
            webClientFactory.createClient(
                baseUrl = "https://openrouter.ai",
                responseTimeoutMs = llmProperties.timeoutMs,
                readTimeoutMs = llmProperties.timeoutMs,
            )
        }

    private data class DailyCounter(
        val date: LocalDate,
        val count: Int,
    )

    private val openRouterCounter = AtomicReference(DailyCounter(LocalDate.now(), 0))

    init {
        log.info { "LLM 사용 가능한 provider: ${llmProperties.availableProviders}" }
    }

    fun call(messages: List<Message>): String? {
        val useOpenRouter = tryUseOpenRouter()
        val provider = if (useOpenRouter) LlmProvider.OPENROUTER else LlmProvider.OLLAMA
        return when (provider) {
            LlmProvider.OLLAMA -> callOllama(messages)
            LlmProvider.OPENROUTER -> callOpenRouter(messages)
        }
    }

    private fun tryUseOpenRouter(): Boolean {
        if (LlmProvider.OPENROUTER !in llmProperties.availableProviders) return false

        val limit = llmProperties.openrouter.dailyLimit
        while (true) {
            val current = openRouterCounter.get()
            val today = LocalDate.now()
            val effective = if (current.date == today) current else DailyCounter(today, 0)
            if (effective.count >= limit) return false
            val next = effective.copy(count = effective.count + 1)
            if (openRouterCounter.compareAndSet(current, next)) {
                if (next.count == limit) {
                    log.warn { "OpenRouter 일일 호출 한도 도달 (${limit}회), Ollama로 전환됩니다." }
                }
                return true
            }
        }
    }

    private fun callOllama(messages: List<Message>): String? {
        val props = llmProperties.ollama
        val request =
            OllamaChatRequest(
                model = props.model,
                messages = messages,
                stream = false,
                options = OllamaOptions(temperature = llmProperties.temperature),
            )

        val client = ollamaClient ?: return null

        val response =
            client
                .post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<OllamaChatResponse>()
                .retryWhen(retrySpec("Ollama"))
                .block()

        return response?.message?.content
    }

    private fun callOpenRouter(messages: List<Message>): String? {
        val props = llmProperties.openrouter
        val request =
            OpenRouterChatRequest(
                model = props.model,
                messages = messages,
                temperature = llmProperties.temperature,
            )

        val client = openRouterClient ?: return null

        val response =
            client
                .post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer ${props.apiKey}")
                .header("HTTP-Referer", "https://safers.pluxity.com")
                .header("X-Title", "SAFERS")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<OpenRouterChatResponse>()
                .retryWhen(retrySpec("OpenRouter"))
                .block()

        return response
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
    }

    private fun retrySpec(provider: String): Retry =
        Retry
            .fixedDelay(1, Duration.ofMillis(500))
            .filter { e ->
                e is WebClientRequestException ||
                    (e is WebClientResponseException && e.statusCode.is5xxServerError)
            }.doBeforeRetry { sig ->
                val cause = sig.failure()
                log.warn { "$provider LLM 재시도 (attempt=${sig.totalRetries() + 1}): ${cause::class.simpleName} - ${cause.message}" }
            }

    companion object {
        val objectMapper: JsonMapper =
            JsonMapper
                .builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()

        fun extractJson(content: String): String {
            // 마크다운 코드블록 제거
            val cleaned =
                content
                    .replace(Regex("```json\\s*"), "")
                    .replace(Regex("```\\s*"), "")
                    .trim()

            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start == -1 || end == -1 || start >= end) {
                throw IllegalStateException("LLM 응답에 JSON이 없습니다: $content")
            }
            val raw = cleaned.substring(start, end + 1)

            // 괄호 불일치 보정
            return repairJson(raw)
        }

        private fun repairJson(json: String): String {
            val sb = StringBuilder()
            val stack = ArrayDeque<Char>()
            var inString = false
            var escape = false

            for (ch in json) {
                if (escape) {
                    sb.append(ch)
                    escape = false
                    continue
                }
                if (ch == '\\' && inString) {
                    sb.append(ch)
                    escape = true
                    continue
                }
                if (ch == '"') {
                    inString = !inString
                    sb.append(ch)
                    continue
                }
                if (inString) {
                    sb.append(ch)
                    continue
                }
                when (ch) {
                    '{', '[' -> {
                        stack.addLast(ch)
                        sb.append(ch)
                    }
                    '}' -> {
                        if (stack.isNotEmpty() && stack.last() == '{') {
                            stack.removeLast()
                            sb.append(ch)
                        }
                    }
                    ']' -> {
                        if (stack.isNotEmpty() && stack.last() == '[') {
                            stack.removeLast()
                            sb.append(ch)
                        }
                    }
                    else -> sb.append(ch)
                }
            }

            while (stack.isNotEmpty()) {
                when (stack.removeLast()) {
                    '{' -> sb.append('}')
                    '[' -> sb.append(']')
                }
            }

            return sb.toString()
        }
    }
}
