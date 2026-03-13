package com.pluxity.weekly.chat.llm

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.core.exception.CustomException
import com.pluxity.weekly.chat.action.dto.LlmAction
import com.pluxity.weekly.chat.config.LlmProperties
import com.pluxity.weekly.chat.llm.dto.IntentResult
import com.pluxity.weekly.chat.llm.dto.OllamaChatRequest
import com.pluxity.weekly.chat.llm.dto.OllamaChatResponse
import com.pluxity.weekly.chat.llm.dto.OllamaMessage
import com.pluxity.weekly.chat.llm.dto.OllamaOptions
import com.pluxity.weekly.chat.llm.dto.OpenAiChatRequest
import com.pluxity.weekly.chat.llm.dto.OpenAiChatResponse
import com.pluxity.weekly.chat.llm.dto.OpenAiMessage
import com.pluxity.weekly.global.constant.WeeklyReportErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger {}

@Service
class LlmService(
    private val properties: LlmProperties,
    private val objectMapper: ObjectMapper,
    webClientFactory: WebClientFactory,
) {
    private val webClient =
        webClientFactory.createClient(
            baseUrl = properties.baseUrl,
        )

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
                val content = callLlm(systemPrompt, userMessage)
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
                val content = callLlm(intentPrompt, message)
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

    internal fun callLlm(
        systemPrompt: String,
        userMessage: String,
    ): String =
        when (properties.provider) {
            "ollama" -> callOllama(systemPrompt, userMessage)
            "openai" -> callOpenAi(systemPrompt, userMessage)
            else -> throw CustomException(WeeklyReportErrorCode.LLM_SERVICE_UNAVAILABLE)
        }

    private fun callOllama(
        systemPrompt: String,
        userMessage: String,
    ): String {
        val request =
            OllamaChatRequest(
                model = properties.model,
                messages =
                    listOf(
                        OllamaMessage(role = "system", content = systemPrompt),
                        OllamaMessage(role = "user", content = userMessage),
                    ),
                options = OllamaOptions(temperature = properties.temperature),
            )

        val response =
            webClient
                .post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaChatResponse::class.java)
                .block()
                ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)

        logOllamaMetrics(response)

        return response.message?.content
            ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
    }

    private fun callOpenAi(
        systemPrompt: String,
        userMessage: String,
    ): String {
        val request =
            OpenAiChatRequest(
                model = properties.model,
                messages =
                    listOf(
                        OpenAiMessage(role = "system", content = systemPrompt),
                        OpenAiMessage(role = "user", content = userMessage),
                    ),
                temperature = properties.temperature,
            )

        val response =
            webClient
                .post()
                .uri("/v1/chat/completions")
                .headers { headers ->
                    if (properties.apiKey.isNotBlank()) {
                        headers.setBearerAuth(properties.apiKey)
                    }
                }.bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiChatResponse::class.java)
                .block()
                ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)

        logOpenAiUsage(response)

        return response.choices
            .firstOrNull()
            ?.message
            ?.content
            ?: throw CustomException(WeeklyReportErrorCode.LLM_INVALID_RESPONSE)
    }

    private fun logOpenAiUsage(response: OpenAiChatResponse) {
        val usage = response.usage ?: return
        log.info {
            "[LLM 사용량] 모델: ${response.model} | " +
                    "프롬프트: ${usage.promptTokens} tokens | " +
                    "생성: ${usage.completionTokens} tokens | " +
                    "총: ${usage.totalTokens} tokens"
        }
    }

    private fun logOllamaMetrics(response: OllamaChatResponse) {
        val promptMs = response.promptEvalDuration / 1_000_000
        val genMs = response.evalDuration / 1_000_000
        val totalMs = response.totalDuration / 1_000_000
        val loadMs = response.loadDuration / 1_000_000
        val tokPerSec = if (genMs > 0) response.evalCount * 1000.0 / genMs else 0.0

        log.info {
            "[LLM 메트릭] 프롬프트: ${response.promptEvalCount} tokens (${promptMs}ms) | " +
                    "생성: ${response.evalCount} tokens (${genMs}ms, ${"%.1f".format(tokPerSec)} tok/s) | " +
                    "총: ${totalMs}ms | 로드: ${loadMs}ms"
        }
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
