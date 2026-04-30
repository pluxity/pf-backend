package com.pluxity.safers.ingest.dto

import com.pluxity.safers.event.entity.EventSeverity
import com.pluxity.safers.event.entity.EventSource
import com.pluxity.safers.event.entity.EventType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "정규화 안전 이벤트 envelope (가스/SOS/스마트밴드)")
data class EventIngestRequest(
    @field:NotBlank
    @field:Schema(example = "GAS-EVT-20260424-0001")
    val eventId: String,
    @field:Schema(description = "EventCategory.SAFETY 인 EventType 만 허용", example = "GAS_THRESHOLD_EXCEEDED")
    val eventType: EventType,
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
    val lon: Double,
    @field:Schema(description = "고도 (m, GPS 측정값)", nullable = true) val alt: Double? = null,
    @field:Schema(description = "수평 정확도 반경 (m)", nullable = true) val accuracyM: Double? = null,
)
