package com.pluxity.safers.llm

import com.pluxity.safers.chat.dto.IntentResult
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.QueryTarget
import com.pluxity.safers.chat.dto.SurfaceUpdate
import com.pluxity.safers.llm.dto.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode

private val log = KotlinLogging.logger {}

@Component
class ChatLlmClient(
    private val llmClient: LlmClient,
) {
    private val objectMapper = LlmClient.objectMapper

    /**
     * 1차 호출: 의도 파악 (summary + actions)
     */
    fun analyzeIntent(messages: List<Message>): IntentResult {
        val start = System.currentTimeMillis()
        val content =
            llmClient.call(messages)
                ?: throw IllegalStateException("LLM 응답이 없습니다")
        val elapsed = System.currentTimeMillis() - start
        log.info { "LLM 1차(의도 파악) - ${elapsed}ms, content: $content" }
        return parseIntentResult(content)
    }

    /**
     * 2차 호출: UI 배치 (surfaceUpdate)
     */
    fun generateLayout(messages: List<Message>): SurfaceUpdate {
        val start = System.currentTimeMillis()
        val content =
            llmClient.call(messages)
                ?: throw IllegalStateException("LLM 응답이 없습니다")
        val elapsed = System.currentTimeMillis() - start
        log.info { "LLM 2차(UI 배치) - ${elapsed}ms, content: $content" }
        return parseSurfaceUpdate(content)
    }

    private fun parseIntentResult(content: String): IntentResult {
        val json = LlmClient.extractJson(content)
        val node = objectMapper.readTree(json)

        return IntentResult(
            summary = node["summary"]?.asString() ?: "",
            actions = parseActions(node["actions"]),
        )
    }

    private fun parseSurfaceUpdate(content: String): SurfaceUpdate {
        val json = LlmClient.extractJson(content)
        val node = objectMapper.readTree(json)

        val surfaceUpdateNode = node["surfaceUpdate"] ?: node
        val surfaceId = surfaceUpdateNode["surfaceId"]?.asString() ?: "main"
        val components = surfaceUpdateNode["components"]?.map { nodeToMap(it) } ?: emptyList()
        return SurfaceUpdate(surfaceId = surfaceId, components = components)
    }

    private fun parseActions(actionsNode: JsonNode?): List<QueryAction> {
        if (actionsNode == null || !actionsNode.isArray) return emptyList()

        return actionsNode.mapNotNull { actionNode ->
            try {
                val targetStr = actionNode["target"].asString().uppercase()
                QueryAction(
                    id = actionNode["id"].asString(),
                    target = QueryTarget.valueOf(targetStr),
                    filters = parseFilters(actionNode["filters"]),
                )
            } catch (e: Exception) {
                log.warn(e) { "chat action 파싱 실패: $actionNode" }
                null
            }
        }
    }

    private fun parseFilters(filtersNode: JsonNode?): Map<String, Any?> {
        if (filtersNode == null || filtersNode.isNull) return emptyMap()

        val filters = mutableMapOf<String, Any?>()
        filtersNode.propertyNames().forEach { fieldName ->
            val value = filtersNode[fieldName]
            filters[fieldName] =
                when {
                    value.isNull -> null
                    value.isString -> value.asString()
                    value.isNumber -> if (value.isInt || value.isLong) value.asLong() else value.asDouble()
                    value.isBoolean -> value.asBoolean()
                    value.isArray -> value.map { it.asString() }
                    else -> value.toString()
                }
        }
        return filters
    }

    private fun nodeToMap(node: JsonNode): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        node.propertyNames().forEach { key ->
            map[key] = nodeToValue(node[key])
        }
        return map
    }

    private fun nodeToValue(node: JsonNode): Any =
        when {
            node.isString -> node.asString()
            node.isNumber -> if (node.isInt || node.isLong) node.asLong() else node.asDouble()
            node.isBoolean -> node.asBoolean()
            node.isArray -> node.map { nodeToValue(it) }
            node.isObject -> nodeToMap(node)
            else -> node.toString()
        }
}
