package com.pluxity.safers.llm

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.llm.dto.EventFilterCriteria
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Component
class LlmClient(
    webClientFactory: WebClientFactory,
    private val llmProperties: LlmProperties,
) {
    private val webClient =
        webClientFactory.createClient(
            baseUrl = llmProperties.baseUrl,
            responseTimeoutMs = llmProperties.timeoutMs,
            readTimeoutMs = llmProperties.timeoutMs,
        )

    private val objectMapper =
        JsonMapper
            .builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    private val isOpenRouter: Boolean
        get() = llmProperties.apiKey.isNotBlank()

    companion object {
        private val EVENT_TYPES_DESC =
            EventType.entries.joinToString("\n") { "- ${it.name}: ${it.displayName}" }

        private fun buildSystemPrompt(today: LocalDate): String =
            """
            |당신은 이벤트 검색 필터를 생성하는 AI입니다.
            |사용자의 자연어 입력을 분석하여 아래 JSON 형식으로만 응답하세요.
            |반드시 JSON만 출력하고, 다른 텍스트는 절대 포함하지 마세요.
            |
            |오늘 날짜: $today
            |
            |## 출력 형식
            |```json
            |{
            |  "startDate": "yyyy-MM-dd'T'HH:mm:ss",
            |  "endDate": "yyyy-MM-dd'T'HH:mm:ss",
            |  "types": ["EVENT_TYPE"]
            |}
            |```
            |
            |## 규칙
            |- 언급되지 않은 필드는 null로 설정
            |- 날짜만 언급된 경우: startDate는 해당일 00:00:00, endDate는 해당일 23:59:59
            |- "어제" = 오늘 - 1일, "오늘" = 오늘, "이번 주" = 이번 주 월요일~오늘
            |- "최근 N일" = 오늘 - N일 ~ 오늘
            |
            |## 이벤트 타입 목록
            |$EVENT_TYPES_DESC
            |
            |## 매핑 규칙
            |- "헬멧 미착용", "안전모 미착용" → NO_HELMET
            |- "헬멧 착용", "안전모 착용" → HELMET
            |- "쓰러진 사람", "넘어진 사람", "쓰러짐" → FALLEN_PERSON
            |- "침입", "영역 침입", "무단 침입" → INTRUSION
            |- "이탈", "영역 이탈" → EXIT
            |- "경계선 통과", "라인 크로싱" → LINE_CROSSING
            """.trimMargin()
    }

    fun parseEventFilter(query: String): EventFilterCriteria? {
        if (llmProperties.baseUrl.isBlank()) {
            log.debug { "LLM base-url이 설정되지 않아 필터 파싱을 건너뜁니다." }
            return null
        }

        val today = LocalDate.now()
        val messages =
            listOf(
                Message(role = "system", content = buildSystemPrompt(today)),
                Message(role = "user", content = query),
            )

        return try {
            val content =
                if (isOpenRouter) {
                    callOpenRouter(messages)
                } else {
                    callOllama(messages)
                }

            content?.let { parseJsonResponse(it) }
        } catch (e: Exception) {
            log.error(e) { "LLM 이벤트 필터 파싱 실패 - query: $query" }
            null
        }
    }

    private fun callOpenRouter(messages: List<Message>): String? {
        val request =
            ChatCompletionRequest(
                model = llmProperties.model,
                temperature = llmProperties.temperature,
                messages = messages,
            )

        val response =
            webClient
                .post()
                .uri("")
                .header("Authorization", "Bearer ${llmProperties.apiKey}")
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
        val request =
            OllamaChatRequest(
                model = llmProperties.model,
                messages = messages,
                stream = false,
                options = OllamaOptions(temperature = llmProperties.temperature),
            )

        val response =
            webClient
                .post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono<OllamaChatResponse>()
                .block()

        return response?.message?.content
    }

    private fun parseJsonResponse(content: String): EventFilterCriteria? {
        val json =
            content
                .replace("```json", "")
                .replace("```", "")
                .trim()

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
                    node.get("types")?.takeIf { !it.isNull && it.isArray }?.map {
                        EventType.valueOf(it.asString())
                    },
            )
        } catch (e: Exception) {
            log.error(e) { "LLM 응답 JSON 파싱 실패 - content: $json" }
            null
        }
    }
}

// OpenRouter (OpenAI 호환)
data class Message(
    val role: String,
    val content: String,
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
)

data class ChatCompletionResponse(
    val choices: List<Choice>,
)

data class Choice(
    val message: ChoiceMessage,
)

data class ChoiceMessage(
    val content: String,
    val role: String? = null,
)

// Ollama
data class OllamaChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean,
    val options: OllamaOptions,
)

data class OllamaOptions(
    val temperature: Double,
)

data class OllamaChatResponse(
    val message: ChoiceMessage,
)
