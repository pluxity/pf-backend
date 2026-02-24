package com.pluxity.safers.global.messaging

import com.pluxity.safers.event.dto.EventResponse
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageHandler(
    private val messageSender: StompMessageSender,
) {
    @MessageMapping("/test/event-created")
    fun testEventCreated(
        @Payload payload: EventResponse,
    ) {
        messageSender.sendEventCreated(payload)
    }

    @MessageMapping("/test/event-video-registered")
    fun testEventVideoRegistered(
        @Payload payload: EventResponse,
    ) {
        messageSender.sendEventVideoRegistered(payload)
    }
}
