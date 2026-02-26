package com.pluxity.safers.event.listener

import com.pluxity.safers.global.messaging.StompMessageSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EventListener(
    private val stompMessageSender: StompMessageSender,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleEventCreated(event: EventCreated) {
        stompMessageSender.sendEventCreated(event.eventResponse)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleEventVideoRegistered(event: EventVideoRegistered) {
        stompMessageSender.sendEventVideoRegistered(event.eventResponse)
    }
}
