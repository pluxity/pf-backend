package com.pluxity.safers.llm

import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.llm.dto.EventFilterCriteria
import com.pluxity.safers.llm.dto.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Component
class EventLlmClient(
    private val llmClient: LlmClient,
) {
    private val objectMapper = LlmClient.objectMapper

    private val promptTemplate: String by lazy {
        ClassPathResource("prompts/event-filter-system.txt").getContentAsString(Charsets.UTF_8)
    }

    companion object {
        private val EVENT_TYPES_DESC =
            EventType.entries.joinToString("\n") { "- ${it.name}: ${it.displayName}" }
    }

    fun parseEventFilter(query: String): EventFilterCriteria? {
        val now = LocalDateTime.now()
        val systemPrompt =
            promptTemplate
                .replace("{{now}}", now.toString())
                .replace("{{eventTypes}}", EVENT_TYPES_DESC)

        val messages =
            listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = query),
            )

        return try {
            val start = System.currentTimeMillis()
            val content = llmClient.call(messages)
            val elapsed = System.currentTimeMillis() - start

            content?.let { parseResponse(it) }?.also {
                log.info { "LLM 이벤트 필터 파싱 완료 - ${elapsed}ms, query: $query, criteria: $it" }
            }
        } catch (e: Exception) {
            log.error(e) { "LLM 이벤트 필터 파싱 실패 - query: $query" }
            null
        }
    }

    private fun parseResponse(content: String): EventFilterCriteria? {
        val json = LlmClient.extractJson(content)

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
                    node
                        .get("types")
                        ?.takeIf { !it.isNull && it.isArray }
                        ?.mapNotNull { runCatching { EventType.valueOf(it.asString()) }.getOrNull() },
            )
        } catch (e: Exception) {
            log.error(e) { "LLM 이벤트 응답 JSON 파싱 실패 - content: $json" }
            null
        }
    }
}
