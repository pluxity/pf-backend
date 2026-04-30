package com.pluxity.safers.ingest.dto

import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.entity.EventSeverity
import com.pluxity.safers.event.entity.EventSource
import com.pluxity.safers.event.entity.EventType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "안전 이벤트 응답 (STOMP 브로드캐스트 + ingest API 응답 공용)")
data class SafetyEventResponse(
    @field:Schema(description = "내부 ID", example = "1024")
    val id: Long,
    @field:Schema(description = "외부 시스템 이벤트 ID", example = "GAS-EVT-20260424-0001")
    val eventId: String,
    @field:Schema(description = "이벤트 유형", example = "GAS_THRESHOLD_EXCEEDED")
    val eventType: EventType,
    @field:Schema(description = "이벤트 이름", example = "가스 임계 초과")
    val name: String,
    @field:Schema(description = "심각도", example = "CRITICAL")
    val severity: EventSeverity,
    @field:Schema(description = "발생원", example = "GAS_SENSOR")
    val source: EventSource,
    @field:Schema(description = "발생 시간", example = "2026-04-24T10:15:30")
    val occurredAt: LocalDateTime,
    @field:Schema(description = "사이트 ID", example = "42")
    val siteId: Long,
    @field:Schema(description = "센서 디바이스 ID (가스/SOS)", example = "GAS-MH203-01", nullable = true)
    val deviceId: String?,
    @field:Schema(description = "스마트밴드 ID", example = "BAND-A1B2C3", nullable = true)
    val bandId: String?,
    @field:Schema(description = "Raw 위도", nullable = true)
    val lat: Double?,
    @field:Schema(description = "Raw 경도", nullable = true)
    val lon: Double?,
    @field:Schema(description = "고도 (m, GPS 측정값)", nullable = true)
    val alt: Double?,
    @field:Schema(description = "수평 정확도 반경 (m)", nullable = true)
    val accuracyM: Double?,
    @field:Schema(description = "이벤트 종류별 상세 (JSONB)", nullable = true)
    val payload: Map<String, Any>?,
)

fun Event.toSafetyResponse(): SafetyEventResponse =
    SafetyEventResponse(
        id = requiredId,
        eventId = eventId,
        eventType = type,
        name = name,
        severity = requireNotNull(severity) { "severity must be set for SAFETY events" },
        source = requireNotNull(source) { "source must be set for SAFETY events" },
        occurredAt = eventTimestamp,
        siteId = siteId,
        deviceId = deviceId,
        bandId = bandId,
        lat = lat,
        lon = lon,
        alt = alt,
        accuracyM = accuracyM,
        payload = payload,
    )
