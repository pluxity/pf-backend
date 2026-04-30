package com.pluxity.safers.event.dto

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.entity.EventSeverity
import com.pluxity.safers.event.entity.EventSource
import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.site.dto.SiteResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "이벤트 응답 (CCTV + SAFETY 통합)")
data class EventResponse(
    @field:Schema(description = "이벤트 ID", example = "1")
    val id: Long,
    @field:Schema(description = "외부 시스템 이벤트 ID", example = "EVT-4cf943c7a2d6")
    val eventId: String,
    @field:Schema(description = "이벤트 발생 시간")
    val timestamp: LocalDateTime,
    @field:Schema(description = "이벤트 카테고리", example = "DETECTION")
    val category: EventCategory,
    @field:Schema(description = "이벤트 유형", example = "NO_HELMET")
    val type: EventType,
    @field:Schema(description = "추적 ID (CCTV 전용)", example = "12345", nullable = true)
    val trackId: Long?,
    @field:Schema(description = "이벤트명", example = "헬멧 미착용 감지")
    val name: String,
    @field:Schema(description = "신뢰도 (CCTV 전용, 0.0 ~ 1.0)", nullable = true, example = "0.95")
    val confidence: Double?,
    @field:Schema(description = "CCTV 스트림 경로", example = "CCTV-JEJU1-46")
    val path: String,
    @field:Schema(description = "현장 정보")
    val site: SiteResponse?,
    @field:Schema(description = "스냅샷 파일 (CCTV)")
    val snapshot: FileResponse?,
    @field:Schema(description = "영상 파일 (CCTV)")
    val video: FileResponse?,
    @field:Schema(description = "심각도 (SAFETY 전용)", nullable = true, example = "CRITICAL")
    val severity: EventSeverity?,
    @field:Schema(description = "발생원 (SAFETY 전용)", nullable = true, example = "GAS_SENSOR")
    val source: EventSource?,
    @field:Schema(description = "센서 디바이스 ID (SAFETY 가스/SOS)", nullable = true, example = "GAS-MH203-01")
    val deviceId: String?,
    @field:Schema(description = "스마트밴드 ID (SAFETY)", nullable = true, example = "BAND-A1B2C3")
    val bandId: String?,
    @field:Schema(description = "Raw 위도 (SAFETY)", nullable = true)
    val lat: Double?,
    @field:Schema(description = "Raw 경도 (SAFETY)", nullable = true)
    val lon: Double?,
    @field:Schema(description = "고도 (m, SAFETY)", nullable = true)
    val alt: Double?,
    @field:Schema(description = "수평 정확도 반경 (m, SAFETY)", nullable = true)
    val accuracyM: Double?,
    @field:Schema(description = "이벤트 종류별 상세 (SAFETY, JSONB)", nullable = true)
    val payload: Map<String, Any>?,
)

fun Event.toResponse(
    snapshotFileResponse: FileResponse?,
    videoFileResponse: FileResponse? = null,
    siteResponse: SiteResponse? = null,
): EventResponse =
    EventResponse(
        id = requiredId,
        eventId = eventId,
        timestamp = eventTimestamp,
        category = category,
        type = type,
        trackId = trackId,
        name = name,
        confidence = confidence,
        path = path,
        site = siteResponse,
        snapshot = snapshotFileResponse ?: FileResponse(),
        video = videoFileResponse ?: FileResponse(),
        severity = severity,
        source = source,
        deviceId = deviceId,
        bandId = bandId,
        lat = lat,
        lon = lon,
        alt = alt,
        accuracyM = accuracyM,
        payload = payload,
    )
