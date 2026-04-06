package com.pluxity.safers.chat.service

import com.pluxity.safers.chat.dto.ChatResponse
import com.pluxity.safers.chat.dto.QueryAction
import com.pluxity.safers.llm.LlmClient
import com.pluxity.safers.llm.dto.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * 캐시된 화면 메타데이터 (프롬프트에 포함용)
 */
data class ScreenMeta(
    val ref: String,
    val summary: String,
    val siteIds: List<Long>,
    val targets: List<String>,
)

/**
 * 캐시된 화면 전체 (복원용)
 */
data class ScreenCache(
    val meta: ScreenMeta,
    val actions: List<QueryAction>,
    val response: ChatResponse,
)

@Component
class ChatHistoryStore(
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val MAX_HISTORY = 10
        private const val MAX_SCREENS = 10
        private const val TTL_HOURS = 24L
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
        redisTemplate.opsForList().trim(key, -MAX_HISTORY.toLong(), -1)
        redisTemplate.expire(key, Duration.ofHours(TTL_HOURS))
    }

    fun incrementTurn(sessionId: String): Long {
        val key = "chat:turn:$sessionId"
        val turn = redisTemplate.opsForValue().increment(key) ?: 1L
        redisTemplate.expire(key, Duration.ofHours(TTL_HOURS))
        return turn
    }

    fun nextScreenRef(sessionId: String): String {
        val key = "chat:screen-seq:$sessionId"
        val seq = redisTemplate.opsForValue().increment(key) ?: 1L
        redisTemplate.expire(key, Duration.ofHours(TTL_HOURS))
        return "h$seq"
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

        val cache =
            ScreenCache(
                meta = ScreenMeta(ref = ref, summary = summary, siteIds = siteIds, targets = targets),
                actions = actions,
                response = response,
            )
        redisTemplate.opsForHash<String, String>().put(key, ref, objectMapper.writeValueAsString(cache))

        // 오래된 캐시 정리: MAX_SCREENS 초과 시 가장 오래된 것 제거
        val size = redisTemplate.opsForHash<String, String>().size(key)
        if (size > MAX_SCREENS) {
            val allKeys = redisTemplate.opsForHash<String, String>().keys(key).sorted()
            val toRemove = allKeys.take((size - MAX_SCREENS).toInt())
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
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.ref }
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
