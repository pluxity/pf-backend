package com.pluxity.yongin.location.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "근로자 위치 정보 응답")
data class WorkerLocationResponse(
    @field:Schema(description = "근로자 ID", example = "1")
    val workerId: Long,
    @field:Schema(description = "위도 (WGS84)", example = "37.2411")
    val latitude: Double,
    @field:Schema(description = "경도 (WGS84)", example = "127.1775")
    val longitude: Double,
    @field:Schema(description = "위치 수집 시각", example = "2026-03-12T09:30:00")
    val timestamp: LocalDateTime,
    @field:Schema(description = "GPS 정밀도 (미터)", example = "3.5")
    val accuracy: Double,
)
