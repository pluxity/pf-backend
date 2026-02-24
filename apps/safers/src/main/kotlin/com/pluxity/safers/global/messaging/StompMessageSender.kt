package com.pluxity.safers.global.messaging

import com.pluxity.safers.event.dto.EventResponse
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
        const val TOPIC_EVENTS: String = "/topic/events"
        const val TOPIC_EVENT_VIDEOS: String = "/topic/event-videos"
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = TOPIC_EVENTS,
                description = "이벤트 생성 시 전체 사용자에게 브로드캐스트",
                payloadType = EventResponse::class,
            ),
    )
    @StompAsyncOperationBinding
    fun sendEventCreated(payload: EventResponse) {
        log.info { "Broadcasting event created to all users: ${payload.eventId}" }
        messageTemplate.convertAndSend(TOPIC_EVENTS, payload)
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = TOPIC_EVENT_VIDEOS,
                description = "이벤트 영상 등록 시 전체 사용자에게 브로드캐스트",
                payloadType = EventResponse::class,
            ),
    )
    @StompAsyncOperationBinding
    fun sendEventVideoRegistered(payload: EventResponse) {
        log.info { "Broadcasting event video registered to all users: ${payload.eventId}" }
        messageTemplate.convertAndSend(TOPIC_EVENT_VIDEOS, payload)
    }
}
