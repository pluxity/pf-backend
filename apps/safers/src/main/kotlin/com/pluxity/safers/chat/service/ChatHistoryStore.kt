package com.pluxity.safers.chat.service

import com.pluxity.safers.chat.dto.ChatResponse
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.chat.dto.ScreenCache
import com.pluxity.safers.chat.dto.ScreenMeta
import com.pluxity.safers.llm.LlmClient
import com.pluxity.safers.llm.dto.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

private val log = KotlinLogging.logger {}

@Component
class ChatHistoryStore(
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val MAX_SIZE = 10
        private const val TTL_HOURS = 6L
    }

    private val objectMapper = LlmClient.objectMapper

    // ── 대화 히스토리 (기존) ──

    fun save(
        sessionId: String,
        role: String,
        content: String,
    ) {
        val key = "chat:history:$sessionId"
        val entry = objectMapper.writeValueAsString(mapOf("role" to role, "content" to content))
        redisTemplate.opsForList().rightPush(key, entry)
        redisTemplate.opsForList().trim(key, -MAX_SIZE.toLong(), -1)
        redisTemplate.expire(key, Duration.ofHours(TTL_HOURS))
    }

    fun incrementTurn(sessionId: String): Long {
        val key = "chat:turn:$sessionId"
        val turn = redisTemplate.opsForValue().increment(key) ?: 1L
        redisTemplate.expire(key, Duration.ofHours(TTL_HOURS))
        return turn
    }

    fun load(sessionId: String): List<Message> {
        val key = "chat:history:$sessionId"
        return try {
            redisTemplate
                .opsForList()
                .range(key, 0, -1)
                ?.mapNotNull { parseMessage(it) }
                ?: emptyList()
        } catch (e: Exception) {
            log.warn(e) { "대화 히스토리 로드 실패: $sessionId" }
            emptyList()
        }
    }

    // ── 화면 캐시 (신규) ──

    fun saveScreen(
        sessionId: String,
        ref: String,
        summary: String,
        actions: List<QueryAction>,
        response: ChatResponse,
    ) {
        val key = "chat:screens:$sessionId"

        val siteIds =
            actions
                .mapNotNull { it.filters["siteId"]?.toString()?.toLongOrNull() }
                .distinct()
        val targets =
            actions
                .map { it.target.name.lowercase() }
                .distinct()

        val actionIds = actions.map { it.id }

        val cache =
            ScreenCache(
                meta = ScreenMeta(ref = ref, summary = summary, siteIds = siteIds, targets = targets, actionIds = actionIds),
                actions = actions,
                response = response,
            )
        redisTemplate.opsForHash<String, String>().put(key, ref, objectMapper.writeValueAsString(cache))

        // 오래된 캐시 정리: MAX_SIZE 초과 시 가장 오래된 것 제거
        val size = redisTemplate.opsForHash<String, String>().size(key)
        if (size > MAX_SIZE) {
            val allKeys = redisTemplate.opsForHash<String, String>().keys(key).sortedBy { it.removePrefix("h").toLongOrNull() ?: 0L }
            val toRemove = allKeys.take((size - MAX_SIZE).toInt())
            toRemove.forEach { redisTemplate.opsForHash<String, String>().delete(key, it) }
        }
        redisTemplate.expire(key, Duration.ofHours(TTL_HOURS))
    }

    fun loadScreen(
        sessionId: String,
        ref: String,
    ): ScreenCache? {
        val key = "chat:screens:$sessionId"
        val json = redisTemplate.opsForHash<String, String>().get(key, ref) ?: return null
        return try {
            objectMapper.readValue(json, ScreenCache::class.java)
        } catch (e: Exception) {
            log.warn(e) { "화면 캐시 파싱 실패: ref=$ref" }
            null
        }
    }

    fun loadScreenMetaList(sessionId: String): List<ScreenMeta> {
        val key = "chat:screens:$sessionId"
        return try {
            redisTemplate
                .opsForHash<String, String>()
                .entries(key)
                .values
                .mapNotNull { json ->
                    try {
                        val node = objectMapper.readTree(json)
                        val metaNode = node["meta"]
                        ScreenMeta(
                            ref = metaNode["ref"].asString(),
                            summary = metaNode["summary"].asString(),
                            siteIds =
                                metaNode["siteIds"]?.map { it.asLong() } ?: emptyList(),
                            targets =
                                metaNode["targets"]?.map { it.asString() } ?: emptyList(),
                            actionIds =
                                metaNode["actionIds"]?.map { it.asString() } ?: emptyList(),
                        )
                    } catch (_: Exception) {
                        null
                    }
                }.sortedBy { it.ref.removePrefix("h").toLongOrNull() ?: 0L }
        } catch (e: Exception) {
            log.warn(e) { "화면 메타데이터 로드 실패: $sessionId" }
            emptyList()
        }
    }

    // ── 내부 ──

    private fun parseMessage(json: String): Message? =
        try {
            val node = objectMapper.readTree(json)
            Message(
                role = node["role"].asString(),
                content = node["content"].asString(),
            )
        } catch (e: Exception) {
            log.warn(e) { "메시지 파싱 실패: $json" }
            null
        }
}
