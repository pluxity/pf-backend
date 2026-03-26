package com.pluxity.weekly.teams.service

import com.pluxity.weekly.teams.dto.Activity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

/**
 * Bot Framework 프로토콜에 따라 serviceUrl로 응답을 POST.
 * 에뮬레이터/Teams 모두 이 방식으로 메시지를 수신한다.
 */
@Component
class TeamsBotReplyClient(
    webClientBuilder: WebClient.Builder,
) {
    private val webClient = webClientBuilder.build()

    fun reply(
        activity: Activity,
        responseBody: Map<String, Any>,
    ) {
        val serviceUrl = activity.serviceUrl?.trimEnd('/')
        val conversationId = activity.conversation?.id

        if (serviceUrl == null || conversationId == null) {
            log.warn { "serviceUrl 또는 conversationId 누락 - 응답 전송 불가" }
            return
        }

        val replyActivity = buildReplyActivity(activity, conversationId, responseBody)
        val encodedConvId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8)
        val uri = URI.create("$serviceUrl/v3/conversations/$encodedConvId/activities")

        log.info { "Reply POST → $uri" }
        log.info { "Reply body → $replyActivity" }

        try {
            val result =
                webClient
                    .post()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .bodyValue(replyActivity)
                    .retrieve()
                    .toBodilessEntity()
                    .block()
            log.info { "Reply 전송 성공: ${result?.statusCode}" }
        } catch (e: WebClientResponseException) {
            log.error { "Reply 전송 실패 (${e.statusCode}): ${e.responseBodyAsString}" }
        } catch (e: Exception) {
            log.error(e) { "Reply 전송 중 예외" }
        }
    }

    private fun buildReplyActivity(
        activity: Activity,
        conversationId: String,
        responseBody: Map<String, Any>,
    ): Map<String, Any> =
        responseBody.toMutableMap().apply {
            put("replyToId", activity.id ?: "")
            put(
                "from",
                mapOf(
                    "id" to (activity.recipient?.id ?: "bot"),
                    "name" to (activity.recipient?.name ?: "Bot"),
                ),
            )
            put(
                "recipient",
                mapOf(
                    "id" to (activity.from?.id ?: ""),
                    "name" to (activity.from?.name ?: ""),
                ),
            )
            put("conversation", mapOf("id" to conversationId))
        }
}
