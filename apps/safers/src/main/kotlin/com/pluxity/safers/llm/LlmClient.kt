package com.pluxity.safers.llm

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.llm.dto.ChatCompletionRequest
import com.pluxity.safers.llm.dto.ChatCompletionResponse
import com.pluxity.safers.llm.dto.EventFilterCriteria
import com.pluxity.safers.llm.dto.Message
import com.pluxity.safers.llm.dto.OllamaChatRequest
import com.pluxity.safers.llm.dto.OllamaChatResponse
import com.pluxity.safers.llm.dto.OllamaOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
                baseUrl = it.baseUrl,
                responseTimeoutMs = llmProperties.timeoutMs,
                readTimeoutMs = llmProperties.timeoutMs,
            )
        }

    private val objectMapper =
        JsonMapper
            .builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    init {
        log.info { "LLM 사용 가능한 provider: ${llmProperties.availableProviders}" }
    }

    private val promptTemplate: String by lazy {
        ClassPathResource("prompts/event-filter-system.txt").getContentAsString(Charsets.UTF_8)
    }

    companion object {
        private val EVENT_TYPES_DESC =
            EventType.entries.joinToString("\n") { "- ${it.name}: ${it.displayName}" }
    }

    private fun buildSystemPrompt(now: LocalDateTime): String =
        promptTemplate
            .replace("{{now}}", now.toString())
            .replace("{{eventTypes}}", EVENT_TYPES_DESC)

    fun parseEventFilter(
        query: String,
        provider: LlmProvider,
    ): EventFilterCriteria? {
        if (provider !in llmProperties.availableProviders) {
            log.warn { "LLM provider '$provider'가 설정되지 않았습니다. 사용 가능: ${llmProperties.availableProviders}" }
            return null
        }

        val now = LocalDateTime.now()
        val messages =
            listOf(
                Message(role = "system", content = buildSystemPrompt(now)),
                Message(role = "user", content = query),
            )

        return try {
            val content =
                when (provider) {
                    LlmProvider.OPENROUTER -> callOpenRouter(messages)
                    LlmProvider.OLLAMA -> callOllama(messages)
                }

            content?.let { parseJsonResponse(it) }?.also {
                log.info { "LLM 필터 파싱 완료 - provider: $provider, query: $query, criteria: $it" }
            }
        } catch (e: Exception) {
            log.error(e) { "LLM 이벤트 필터 파싱 실패 - provider: $provider, query: $query" }
            null
        }
    }

    private fun callOpenRouter(messages: List<Message>): String? {
        val props = llmProperties.openrouter
        val request =
            ChatCompletionRequest(
                model = props.model,
                temperature = llmProperties.temperature,
                messages = messages,
            )

        val client = openRouterClient ?: return null

        val response =
            client
                .post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer ${props.apiKey}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<ChatCompletionResponse>()
                .block()

        return response
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
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
                .block()

        return response?.message?.content
    }

    private fun parseJsonResponse(content: String): EventFilterCriteria? {
        val json = extractJson(content)

        return try {
            val node = objectMapper.readTree(json)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

            EventFilterCriteria(
                startDate =
                    node
                        .get("startDate")
                        ?.takeIf { !it.isNull }
                        ?.asString()
                        ?.let { LocalDateTime.parse(it, formatter) },
                endDate =
                    node
                        .get("endDate")
                        ?.takeIf { !it.isNull }
                        ?.asString()
                        ?.let { LocalDateTime.parse(it, formatter) },
                types =
                    node.get("types")?.takeIf { !it.isNull && it.isArray }?.mapNotNull {
                        runCatching { EventType.valueOf(it.asString()) }.getOrNull()
                    },
            )
        } catch (e: Exception) {
            log.error(e) { "LLM 응답 JSON 파싱 실패 - content: $json" }
            null
        }
    }

    private fun extractJson(content: String): String {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        return if (start != -1 && end != -1 && start < end) {
            content.substring(start, end + 1)
        } else {
            content.trim()
        }
    }
}
