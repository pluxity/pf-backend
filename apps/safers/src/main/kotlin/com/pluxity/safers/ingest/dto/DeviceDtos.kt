package com.pluxity.safers.ingest.dto

import com.pluxity.safers.ingest.entity.Device
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

enum class DeviceType { GAS, BAND, SOS }

enum class DeviceStatus { ACTIVE, OFFLINE, RETIRED }

@Schema(description = "통합 디바이스 등록 요청 (type 디스크리미네이터 + metadata jsonb). siteId 는 URL path.")
data class DeviceCreateRequest(
    @field:NotBlank
    @field:Schema(example = "BAND-A1B2C3")
    val id: String,
    val type: DeviceType,
    @field:Schema(example = "1조 작업자", nullable = true)
    val name: String? = null,
    @field:Schema(description = "type 별 자유 필드 (gas: facilityId/installLocal/floor, band: assignedWorkerId 등)")
    val metadata: Map<String, Any> = emptyMap(),
)

@Schema(description = "디바이스 부분 수정 요청")
data class DeviceUpdateRequest(
    @field:Schema(nullable = true) val name: String? = null,
    @field:Schema(nullable = true) val status: DeviceStatus? = null,
    @field:Schema(nullable = true) val metadata: Map<String, Any>? = null,
)

@Schema(description = "디바이스 응답")
data class DeviceResponse(
    val id: String,
    val type: DeviceType,
    val siteId: Long,
    val name: String?,
    val status: DeviceStatus,
    val lastSeenAt: LocalDateTime?,
    val metadata: Map<String, Any>,
)

fun Device.toResponse(): DeviceResponse =
    DeviceResponse(
        id = deviceId,
        type = type,
        siteId = siteId,
        name = name,
        status = status,
        lastSeenAt = lastSeenAt,
        metadata = metadata,
    )
