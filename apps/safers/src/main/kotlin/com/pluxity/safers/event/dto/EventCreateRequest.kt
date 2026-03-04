package com.pluxity.safers.event.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.pluxity.safers.event.entity.EventCategory
import com.pluxity.safers.event.entity.EventType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "이벤트 등록 요청")
data class EventCreateRequest(
    @field:NotBlank
    @field:JsonProperty("event_id")
    @field:Schema(description = "외부 시스템 이벤트 ID", example = "EVT-20240101-001")
    val eventId: String,
    @field:Schema(description = "이벤트 발생 시간", example = "2024-01-01T12:00:00")
    val timestamp: LocalDateTime,
    @field:Schema(description = "이벤트 카테고리", example = "DETECTION")
    val category: EventCategory,
    @field:Schema(description = "이벤트 유형", example = "NO_HELMET")
    val type: EventType,
    @field:JsonProperty("track_id")
    @field:Schema(description = "추적 ID", example = "12345")
    val trackId: Long,
    @field:NotBlank
    @field:Schema(description = "이벤트 이름", example = "헬멧 미착용 감지")
    val name: String,
    @field:NotBlank
    @field:Schema(description = "스냅샷 URL", example = "http://localhost:8080/snapshots/snapshot_001.jpg")
    val snapshot: String,
    @field:Schema(description = "바운딩 박스 좌표 [x1, y1, x2, y2]", example = "[100, 200, 300, 400]")
    val bbox: List<Int>? = null,
    @field:Schema(description = "중심점 좌표")
    val center: Center? = null,
    @field:Schema(description = "신뢰도 (0.0 ~ 1.0)", example = "0.95")
    val confidence: Double? = null,
    @field:Schema(description = "MediaMTX 재생 경로", example = "cam01")
    val path: String = "",
) {
    @Schema(description = "중심점 좌표")
    data class Center(
        @field:Schema(description = "X 좌표", example = "150.5")
        val x: Double,
        @field:Schema(description = "Y 좌표", example = "200.0")
        val y: Double,
    )
}
