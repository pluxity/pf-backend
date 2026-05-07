package com.pluxity.safersCollect.forwarder

import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class EventForwarder(
    private val client: CentralIngestClient,
) {
    fun forward(envelope: EventEnvelope) {
        try {
            client.postEvent(envelope)
        } catch (e: Exception) {
            log.error(e) { "event forward 실패 — eventId=${envelope.eventId} type=${envelope.eventType}" }
            throw e
        }
    }
}
