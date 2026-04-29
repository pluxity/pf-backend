package com.pluxity.safers.ingest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "정규화 이벤트 envelope")
data class EventIngestRequest(
    @field:NotBlank
    @field:Schema(example = "GAS-EVT-20260424-0001")
    val eventId: String,
    @field:Schema(example = "GAS_THRESHOLD_EXCEEDED")
    val eventType: EventTypeKind,
    val severity: EventSeverity,
    val source: EventSource,
    @field:Schema(example = "2026-04-24T10:15:30")
    val occurredAt: LocalDateTime,
    @field:Schema(example = "GAS-MH203-01", nullable = true)
    val deviceId: String? = null,
    @field:Schema(example = "BAND-A1B2C3", nullable = true)
    val bandId: String? = null,
    @field:Schema(nullable = true)
    val rawPosition: RawPosition? = null,
    @field:Schema(description = "이벤트 종류별 상세 (JSONB 저장)")
    val payload: Map<String, Any>,
)

@Schema(description = "기기가 측정 가능한 raw 위치")
data class RawPosition(
    val lat: Double,
    val lng: Double,
    @field:Schema(nullable = true) val accuracyM: Double? = null,
)

enum class EventTypeKind {
    GAS_THRESHOLD_EXCEEDED,
    SOS_TRIGGERED,
    BAND_VITAL_ABNORMAL,
    BAND_FALL_DETECTED,
    BAND_OFFLINE,
}

enum class EventSeverity { INFO, WARNING, CRITICAL }

enum class EventSource { GAS_SENSOR, SOS_DEVICE, SMART_BAND }
