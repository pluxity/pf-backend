package com.pluxity.safers.event.kafka

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.collect.service.CctvEventCollector
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.service.EventFacade
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@Component
class CctvEventConsumer(
    private val eventFacade: EventFacade,
) {
    @KafkaListener(topics = [CctvEventCollector.TOPIC_EVENTS], groupId = "safers", containerFactory = "cctvEventListenerFactory")
    fun consumeEvent(request: EventCreateRequest) {
        logger.info { "CCTV 이벤트 소비: eventId=${request.eventId}" }
        eventFacade.create(request)
    }

    @KafkaListener(topics = [CctvEventCollector.TOPIC_VIDEOS], groupId = "safers", containerFactory = "cctvVideoListenerFactory")
    fun consumeVideo(message: CctvVideoMessage) {
        logger.info { "CCTV 영상 소비: eventId=${message.eventId}" }
        try {
            eventFacade.uploadVideoByEventId(message.eventId, message.videoUrl)
        } catch (e: RetryableException) {
            logger.warn { "영상 소비 실패, 3초 후 재시도 예약: eventId=${message.eventId} — ${e.message}" }
            scheduleVideoRetry(message)
        }
    }

    private fun scheduleVideoRetry(message: CctvVideoMessage) {
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute {
            try {
                eventFacade.uploadVideoByEventId(message.eventId, message.videoUrl)
                logger.info { "영상 재시도 성공: eventId=${message.eventId}" }
            } catch (e: Exception) {
                logger.error { "영상 재시도 최종 실패: eventId=${message.eventId} — ${e.message}" }
            }
        }
    }
}
