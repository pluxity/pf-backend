package com.pluxity.safers.event.kafka

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.collect.service.CctvEventCollector
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.service.EventFacade
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

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
        eventFacade.uploadVideoByEventId(message.eventId, message.videoUrl)
    }
}
