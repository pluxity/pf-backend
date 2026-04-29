package com.pluxity.safers.collect.service

import com.pluxity.safers.collect.dto.CctvVideoCollectRequest
import com.pluxity.safers.collect.event.CctvEventCollected
import com.pluxity.safers.collect.event.CctvVideoCollected
import com.pluxity.safers.event.dto.EventCreateRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class CctvEventCollector(
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun collect(request: EventCreateRequest) {
        logger.info { "CCTV 이벤트 수집 발행: eventId=${request.eventId}" }
        eventPublisher.publishEvent(CctvEventCollected(request))
    }

    fun collectVideo(request: CctvVideoCollectRequest) {
        logger.info { "CCTV 영상 수집 발행: eventId=${request.eventId}" }
        eventPublisher.publishEvent(CctvVideoCollected(eventId = request.eventId, videoUrl = request.video))
    }
}
