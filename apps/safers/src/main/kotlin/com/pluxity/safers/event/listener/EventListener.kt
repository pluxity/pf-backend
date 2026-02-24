package com.pluxity.safers.event.listener

import com.pluxity.safers.global.messaging.StompMessageSender
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class EventListener(
    private val stompMessageSender: StompMessageSender,
) {
    @Async
    @EventListener
    fun handleEventCreated(event: EventCreated) {
        stompMessageSender.sendEventCreated(event.eventResponse)
    }

    @Async
    @EventListener
    fun handleEventVideoRegistered(event: EventVideoRegistered) {
        stompMessageSender.sendEventVideoRegistered(event.eventResponse)
    }
}
