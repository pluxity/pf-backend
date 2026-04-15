package com.pluxity.safers.llm

import com.pluxity.safers.chat.dto.ActionFilter
import com.pluxity.safers.chat.dto.IntentMode
import com.pluxity.safers.chat.dto.IntentResult
import com.pluxity.safers.chat.dto.PatchAction
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.QueryTarget
import com.pluxity.safers.chat.dto.SurfaceUpdateMessage
import com.pluxity.safers.event.entity.EventType
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
    fun generateLayout(messages: List<Message>): SurfaceUpdateMessage {
        val start = System.currentTimeMillis()
        val content =
            llmClient.call(messages)
                ?: throw IllegalStateException("LLM 응답이 없습니다")
        val elapsed = System.currentTimeMillis() - start
        log.info { "LLM 2차(UI 배치) - ${elapsed}ms, content: $content" }
        return parseSurfaceUpdateMessage(content)
    }

    private fun parseIntentResult(content: String): IntentResult {
        val json = LlmClient.extractJson(content)
        val node = objectMapper.readTree(json)

        val modeStr = node["mode"]?.asString()?.uppercase() ?: "NEW"
        val mode =
            try {
                IntentMode.valueOf(modeStr)
            } catch (_: IllegalArgumentException) {
                IntentMode.NEW
            }

        return IntentResult(
            summary = node["summary"]?.asString() ?: "",
            mode = mode,
            actions = parseActions(node["actions"]),
            ref = node["ref"]?.asString(),
            patch =
                node["patch"]?.let { patchNode ->
                    PatchAction(
                        add = parseActions(patchNode["add"]),
                        remove = patchNode["remove"]?.map { it.asString() } ?: emptyList(),
                    )
                },
        )
    }

    private fun parseSurfaceUpdateMessage(content: String): SurfaceUpdateMessage {
        val json = LlmClient.extractJson(content)
        val node = objectMapper.readTree(json)

        val surfaceUpdateNode = node["surfaceUpdate"] ?: node
        val surfaceId = surfaceUpdateNode["surfaceId"]?.asString() ?: "main"
        val components = surfaceUpdateNode["components"]?.map { nodeToMap(it) } ?: emptyList()
        return SurfaceUpdateMessage(surfaceId = surfaceId, components = components)
    }

    private fun parseActions(actionsNode: JsonNode?): List<QueryAction> {
        if (actionsNode == null || !actionsNode.isArray) return emptyList()

        return actionsNode.mapNotNull { actionNode ->
            try {
                val targetStr = actionNode["target"].asString().uppercase()
                val target = QueryTarget.valueOf(targetStr)
                val filtersNode = actionNode["filters"]
                QueryAction(
                    id = actionNode["id"].asString(),
                    target = target,
                    filters = parseActionFilter(target, filtersNode),
                    page = actionNode["page"]?.asInt() ?: 1,
                    size = actionNode["size"]?.asInt() ?: 50,
                )
            } catch (e: Exception) {
                log.warn(e) { "chat action 파싱 실패: $actionNode" }
                null
            }
        }
    }

    private fun parseActionFilter(
        target: QueryTarget,
        filtersNode: JsonNode?,
    ): ActionFilter =
        when (target) {
            QueryTarget.EVENT ->
                ActionFilter.Event(
                    siteId = filtersNode?.get("siteId")?.asLong(),
                    types =
                        filtersNode?.get("types")?.mapNotNull { typeNode ->
                            runCatching { EventType.valueOf(typeNode.asString()) }.getOrNull()
                        },
                    startDate = filtersNode?.get("startDate")?.asString(),
                    endDate = filtersNode?.get("endDate")?.asString(),
                )
            QueryTarget.CCTV ->
                ActionFilter.Cctv(
                    name = filtersNode?.get("name")?.asString(),
                    siteId = filtersNode?.get("siteId")?.asLong(),
                )
            QueryTarget.WEATHER ->
                ActionFilter.Weather(
                    siteId =
                        filtersNode?.get("siteId")?.asLong()
                            ?: throw IllegalArgumentException("weather action은 siteId가 필수입니다"),
                )
            QueryTarget.SITE ->
                ActionFilter.Site(
                    siteId = filtersNode?.get("siteId")?.asLong(),
                )
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
