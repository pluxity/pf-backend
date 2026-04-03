package com.pluxity.safers.chat.service

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
        private const val MAX_HISTORY = 20
        private const val TTL_HOURS = 24L
    }

    private val objectMapper = LlmClient.objectMapper

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
