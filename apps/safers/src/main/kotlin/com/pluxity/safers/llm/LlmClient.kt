package com.pluxity.safers.llm

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.llm.dto.EventFilterCriteria
import io.github.oshai.kotlinlogging.KotlinLogging
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

    companion object {
        private val EVENT_TYPES_DESC =
            EventType.entries.joinToString("\n") { "- ${it.name}: ${it.displayName}" }

        private fun buildSystemPrompt(now: LocalDateTime): String =
            """
            |당신은 이벤트 검색 필터를 생성하는 AI입니다.
            |사용자의 자연어 입력을 분석하여 아래 JSON 형식으로만 응답하세요.
            |반드시 JSON만 출력하고, 다른 텍스트는 절대 포함하지 마세요.
            |
            |현재 날짜/시각: $now
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
            |
            |## 날짜 표현
            |- "오늘" = 오늘
            |- "어제" = 오늘 - 1일
            |- "그저께", "엊그제" = 오늘 - 2일
            |- "이번 주" = 이번 주 월요일 ~ 오늘
            |- "지난주" = 지난 주 월요일 ~ 지난 주 일요일
            |- "이번 달" = 이번 달 1일 ~ 오늘
            |- "지난달" = 지난 달 1일 ~ 지난 달 마지막 날
            |- "최근 N일" = 오늘 - N일 ~ 오늘
            |- "N월 N일", "3월 15일" 등 = 해당 날짜 (연도 생략 시 올해)
            |- "주말" = 가장 최근 토요일 ~ 일요일
            |- "지난주 화요일" 등 요일 지정 = 해당 요일
            |
            |## 시간대 표현
            |- "새벽" = 00:00:00 ~ 05:59:59
            |- "아침" = 06:00:00 ~ 08:59:59
            |- "오전" = 00:00:00 ~ 11:59:59
            |- "낮" = 12:00:00 ~ 17:59:59
            |- "오후" = 12:00:00 ~ 23:59:59
            |- "점심", "점심시간" = 11:00:00 ~ 13:59:59
            |- "저녁" = 18:00:00 ~ 20:59:59
            |- "밤" = 21:00:00 ~ 23:59:59
            |- "한밤중", "자정" = 23:00:00 ~ 다음날 01:00:00
            |- "출근 시간", "출근 무렵" = 06:00:00 ~ 10:00:00
            |- "퇴근 시간", "퇴근 무렵" = 16:00:00 ~ 20:00:00
            |- "N시간 전" = 현재 시각 - N시간 ~ 현재 시각
            |- "방금", "조금 전" = 현재 시각 - 1시간 ~ 현재 시각
            |
            |## 구간/근사 표현
            |- "N시부터 N시" = 명시된 시간 구간 (예: "2시부터 5시" = 14:00:00 ~ 17:00:00)
            |- "N시쯤", "N시경" = 해당 시각 ±30분 (예: "오후 3시쯤" = 14:30:00 ~ 15:30:00)
            |- "점심시간 이후", "오후부터" 등 열린 끝 표현 = 해당 시작 시각 ~ 현재 시각
            |
            |## 복합 표현
            |- 날짜 + 시간대를 조합하여 처리 (예: "어제 오후" = 어제 12:00:00 ~ 어제 23:59:59)
            |- "오늘 새벽" = 오늘 00:00:00 ~ 오늘 05:59:59
            |- "어젯밤" = 어제 21:00:00 ~ 어제 23:59:59
            |- "오늘 밤" = 오늘 21:00:00 ~ 오늘 23:59:59
            |- "어제 새벽" = 어제 00:00:00 ~ 어제 05:59:59
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
                    node.get("types")?.takeIf { !it.isNull && it.isArray }?.mapNotNull {
                        runCatching { EventType.valueOf(it.asString()) }.getOrNull()
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
    val choices: List<Choice>? = null,
)

data class Choice(
    val message: ChoiceMessage? = null,
)

data class ChoiceMessage(
    val content: String? = null,
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
    val message: ChoiceMessage? = null,
)
