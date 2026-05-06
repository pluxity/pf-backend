package com.pluxity.safersCollect.forwarder.dto

import com.pluxity.safersCollect.forwarder.enums.SourceType
import java.time.LocalDateTime

data class TelemetryEnvelope(
    val sourceType: SourceType,
    val sourceId: String,
    val timestamp: LocalDateTime,
    val measurement: String,
    val tags: Map<String, String> = emptyMap(),
    val fields: Map<String, Any> = emptyMap(),
)
