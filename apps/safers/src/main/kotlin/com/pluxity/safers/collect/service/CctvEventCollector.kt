package com.pluxity.safers.collect.service

import com.pluxity.safers.collect.dto.CctvVideoMessage
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventVideoUploadRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class CctvEventCollector(
    private val eventKafkaTemplate: KafkaTemplate<String, EventCreateRequest>,
    private val videoKafkaTemplate: KafkaTemplate<String, CctvVideoMessage>,
) {
    companion object {
        const val TOPIC_EVENTS = "plx-cctv-events"
        const val TOPIC_VIDEOS = "plx-cctv-event-videos"
    }

    fun collect(request: EventCreateRequest) {
        eventKafkaTemplate
            .send(TOPIC_EVENTS, request.eventId, request)
            .whenComplete { _, ex ->
                if (ex != null) {
                    logger.error(ex) { "CCTV 이벤트 발행 실패: eventId=${request.eventId}" }
                } else {
                    logger.info { "CCTV 이벤트 수집 발행: eventId=${request.eventId}" }
                }
            }
    }

    fun collectVideo(
        eventId: Long,
        request: EventVideoUploadRequest,
    ) {
        val message = CctvVideoMessage(eventId = eventId, videoUrl = request.video)
        videoKafkaTemplate
            .send(TOPIC_VIDEOS, eventId.toString(), message)
            .whenComplete { _, ex ->
                if (ex != null) {
                    logger.error(ex) { "CCTV 영상 발행 실패: eventId=$eventId" }
                } else {
                    logger.info { "CCTV 영상 수집 발행: eventId=$eventId" }
                }
            }
    }
}
