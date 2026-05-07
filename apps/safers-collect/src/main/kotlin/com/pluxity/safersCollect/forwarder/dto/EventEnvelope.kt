package com.pluxity.safersCollect.forwarder.dto

import com.pluxity.safersCollect.forwarder.enums.WireEventSource
import com.pluxity.safersCollect.forwarder.enums.WireEventType
import com.pluxity.safersCollect.v1.collect.enums.EventSeverity
import java.time.LocalDateTime

// safers 모듈의 EventIngestRequest 와 wire 호환.
data class EventEnvelope(
    val eventId: String,
    val eventType: WireEventType,
    val severity: EventSeverity,
    val source: WireEventSource,
    val occurredAt: LocalDateTime,
    val deviceId: String? = null,
    val bandId: String? = null,
    val rawPosition: WireRawPosition? = null,
    val payload: Map<String, Any>,
)

data class WireRawPosition(
    val lat: Double,
    val lon: Double,
    val alt: Double? = null,
    val accuracyM: Double? = null,
)
