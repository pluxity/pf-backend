package com.pluxity.weekly.chat.llm

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.chat.config.LlmProperties
import com.pluxity.weekly.chat.dto.LlmAction
import com.pluxity.weekly.chat.llm.dto.GeminiContent
import com.pluxity.weekly.chat.llm.dto.GeminiGenerationConfig
import com.pluxity.weekly.chat.llm.dto.GeminiPart
import com.pluxity.weekly.chat.llm.dto.GeminiRequest
import com.pluxity.weekly.chat.llm.dto.GeminiResponse
import com.pluxity.weekly.chat.llm.dto.IntentResult
import com.pluxity.weekly.chat.llm.dto.Message
import com.pluxity.weekly.chat.llm.dto.OllamaChatRequest
import com.pluxity.weekly.chat.llm.dto.OllamaChatResponse
import com.pluxity.weekly.chat.llm.dto.OllamaOptions
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

// TODO: 일일 토큰 사용량 제한 및 모니터링 추가

@Service
class LlmService(
    private val properties: LlmProperties,
    private val objectMapper: ObjectMapper,
    webClientFactory: WebClientFactory,
) {
    private val ollamaClient: WebClient? =
        properties.ollama.takeIf { it.isEnabled }?.let {
            webClientFactory.createClient(
                baseUrl = it.baseUrl,
                responseTimeoutMs = properties.timeoutMs,
                readTimeoutMs = properties.timeoutMs,
            )
        }

    private val geminiClient: WebClient? =
        properties.gemini.takeIf { it.isEnabled }?.let {
            webClientFactory.createClient(
                baseUrl = "https://generativelanguage.googleapis.com",
                responseTimeoutMs = properties.timeoutMs,
                readTimeoutMs = properties.timeoutMs,
            )
        }

    init {
        log.info { "LLM Ollama 활성화: ${properties.ollama.isEnabled}, Gemini 활성화: ${properties.gemini.isEnabled}" }
    }

    private val systemPrompt: String by lazy {
        ClassPathResource("llm/system-prompt.txt").getContentAsString(Charsets.UTF_8)
    }

    private val intentPrompt: String by lazy {
        ClassPathResource("llm/intent-prompt.txt").getContentAsString(Charsets.UTF_8)
    }

    fun generate(userMessage: String): List<LlmAction> {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val messages =
                    listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = userMessage),
                    )
                val content = callGemini(messages)
                log.info { "llm response : $content" }

                return parseActions(content)
            } catch (e: CustomException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                log.warn { "LLM 호출 실패 (시도 ${attempt + 1}/$MAX_RETRIES): ${e.message}" }
            }
        }
        log.error(lastException) { "LLM 서비스 $MAX_RETRIES 회 재시도 실패" }
        throw CustomException(WeeklyReportErrorCode.LLM_SERVICE_UNAVAILABLE)
    }

    fun extractIntent(message: String): IntentResult {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val messages =
                    listOf(
                        Message(role = "system", content = intentPrompt),
                        Message(role = "user", content = message),
                    )
                val content = callGemini(messages)
                log.info { "intent response : $content" }
                return parseIntent(content)
            } catch (e: CustomException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                log.warn { "Intent 추출 실패 (시도 ${attempt + 1}/$MAX_RETRIES): ${e.message}" }
            }
        }
        log.error(lastException) { "Intent 추출 $MAX_RETRIES 회 재시도 실패" }
        throw CustomException(WeeklyReportErrorCode.LLM_SERVICE_UNAVAILABLE)
    }

    private fun callOllama(messages: List<Message>): String {
        val props = properties.ollama
        val request =
            OllamaChatRequest(
                model = props.model,
                messages = messages,
                stream = false,
                options = OllamaOptions(temperature = properties.temperature),
            )

        val client =
            ollamaClient
                ?: throw CustomException(WeeklyReportErrorCode.LLM_SERVICE_UNAVAILABLE)

        val response =
            client
                .post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<OllamaChatResponse>()
                .block()
                ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)

        return response.message?.content
            ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
    }

    private fun callGemini(messages: List<Message>): String {
        val props = properties.gemini
        val systemMessage = messages.firstOrNull { it.role == "system" }
        val userMessages = messages.filter { it.role != "system" }

        val request =
            GeminiRequest(
                systemInstruction =
                    systemMessage?.let {
                        GeminiContent(parts = listOf(GeminiPart(text = it.content)))
                    },
                contents =
                    userMessages.map {
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = it.content)),
                        )
                    },
                generationConfig = GeminiGenerationConfig(temperature = properties.temperature),
            )

        val client =
            geminiClient
                ?: throw CustomException(WeeklyReportErrorCode.LLM_SERVICE_UNAVAILABLE)

        val response =
            client
                .post()
                .uri("/v1beta/models/${props.model}:generateContent")
                .header("x-goog-api-key", props.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<GeminiResponse>()
                .block()
                ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)

        return response
            .candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
    }

    private fun parseActions(raw: String): List<LlmAction> {
        val json = stripCodeFence(raw).trim()
        if (json.isBlank()) {
            throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
        }

        return try {
            if (json.trimStart().startsWith("[")) {
                objectMapper.readValue(
                    json,
                    objectMapper.typeFactory.constructCollectionType(List::class.java, LlmAction::class.java),
                )
            } else {
                listOf(objectMapper.readValue(json, LlmAction::class.java))
            }
        } catch (_: Exception) {
            log.error { "LLM 응답 JSON 파싱 실패: $json" }
            throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
        }
    }

    internal fun parseIntent(raw: String): IntentResult {
        val json = stripCodeFence(raw).trim()
        if (json.isBlank()) {
            throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
        }

        return try {
            objectMapper.readValue(json, IntentResult::class.java)
        } catch (_: Exception) {
            log.error { "Intent JSON 파싱 실패: $json" }
            throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
        }
    }

    companion object {
        private const val MAX_RETRIES = 3

        fun stripCodeFence(raw: String): String {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("```")) return trimmed
            val lines = trimmed.lines()
            val start = 1
            val end = if (lines.last().trim() == "```") lines.size - 1 else lines.size
            return lines.subList(start, end).joinToString("\n")
        }
    }
}
