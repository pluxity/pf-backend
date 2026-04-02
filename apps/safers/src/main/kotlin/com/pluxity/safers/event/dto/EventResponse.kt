package com.pluxity.safers.event.dto

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.entity.EventType
import com.pluxity.safers.site.dto.SiteResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "이벤트 응답")
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
    @field:Schema(description = "추적 ID", example = "12345")
    val trackId: Long,
    @field:Schema(description = "이벤트명", example = "헬멧 미착용 감지")
    val name: String,
    @field:Schema(description = "신뢰도 (0.0 ~ 1.0)", example = "0.95")
    val confidence: Double?,
    @field:Schema(description = "CCTV 스트림 경로", example = "CCTV-JEJU1-46")
    val path: String,
    @field:Schema(description = "현장 정보")
    val site: SiteResponse?,
    @field:Schema(description = "스냅샷 파일")
    val snapshot: FileResponse?,
    @field:Schema(description = "영상 파일")
    val video: FileResponse?,
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
    )
