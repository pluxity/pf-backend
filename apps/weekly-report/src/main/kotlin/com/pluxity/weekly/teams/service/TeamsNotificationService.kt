package com.pluxity.weekly.teams.service

import com.pluxity.weekly.teams.repository.TeamsConversationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class TeamsNotificationService(
    private val replyClient: TeamsBotReplyClient,
    private val referenceRepository: TeamsConversationRepository,
) {
    fun sendDm(
        userId: Long,
        message: String,
    ) {
        val reference = referenceRepository.findByUserId(userId)
        if (reference == null) {
            log.warn { "Teams 알림 전송 실패 - userId=$userId 의 conversationReference 없음" }
            return
        }

        replyClient.sendProactive(reference.serviceUrl, reference.conversationId, message)
    }
}
