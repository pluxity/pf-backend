package com.pluxity.safers.global.messaging

import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.ingest.dto.SafetyEventResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.springwolf.bindings.stomp.annotations.StompAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class StompMessageSender(
    private val messageTemplate: SimpMessagingTemplate,
) {
    companion object {
        const val TOPIC_CCTV_EVENTS: String = "/topic/events"
        const val TOPIC_CCTV_EVENT_VIDEOS: String = "/topic/event-videos"
        const val TOPIC_SAFETY_EVENTS: String = "/topic/safety-events"
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = TOPIC_CCTV_EVENTS,
                description = "CCTV 이벤트 생성 시 전체 사용자에게 브로드캐스트",
                payloadType = EventResponse::class,
            ),
    )
    @StompAsyncOperationBinding
    fun sendCctvEventCreated(payload: EventResponse) {
        log.info { "Broadcasting CCTV event created to all users: ${payload.eventId}" }
        messageTemplate.convertAndSend(TOPIC_CCTV_EVENTS, payload)
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = TOPIC_CCTV_EVENT_VIDEOS,
                description = "CCTV 이벤트 영상 등록 시 전체 사용자에게 브로드캐스트",
                payloadType = EventResponse::class,
            ),
    )
    @StompAsyncOperationBinding
    fun sendCctvEventVideoRegistered(payload: EventResponse) {
        log.info { "Broadcasting CCTV event video registered to all users: ${payload.eventId}" }
        messageTemplate.convertAndSend(TOPIC_CCTV_EVENT_VIDEOS, payload)
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = TOPIC_SAFETY_EVENTS,
                description = "안전 이벤트(가스/SOS/밴드) 생성 시 전체 사용자에게 브로드캐스트",
                payloadType = SafetyEventResponse::class,
            ),
    )
    @StompAsyncOperationBinding
    fun sendSafetyEventCreated(payload: SafetyEventResponse) {
        log.info { "Broadcasting safety event created to all users: ${payload.eventId}" }
        messageTemplate.convertAndSend(TOPIC_SAFETY_EVENTS, payload)
    }
}
