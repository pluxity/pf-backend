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
    fun handleCctvEventCreated(event: CctvEventCreated) {
        stompMessageSender.sendCctvEventCreated(event.eventResponse)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCctvEventVideoRegistered(event: CctvEventVideoRegistered) {
        stompMessageSender.sendCctvEventVideoRegistered(event.eventResponse)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleSafetyEventCreated(event: SafetyEventCreated) {
        stompMessageSender.sendSafetyEventCreated(event.eventResponse)
    }
}
