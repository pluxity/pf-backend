package com.pluxity.safersCollect.v1.devices.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "스마트밴드 디바이스 metadata")
data class BandDeviceMetadata(
    @field:Schema(example = "Acme", nullable = true) val vendor: String? = null,
    @field:Schema(example = "BandPro2", nullable = true) val model: String? = null,
    @field:Schema(example = "1.2.3", nullable = true) val firmware: String? = null,
)

@Schema(description = "스마트밴드 등록 요청")
data class BandDeviceCreateRequest(
    @field:NotBlank @field:Schema(example = "BAND-A1B2C3") val bandId: String,
    @field:NotBlank @field:Schema(example = "1조 작업자") val name: String,
    @field:Schema(example = "W102", nullable = true) val assignedWorkerId: String? = null,
    @field:Schema(nullable = true) val metadata: BandDeviceMetadata? = null,
)

@Schema(description = "스마트밴드 부분 수정 요청 (워커 재할당 등)")
data class BandDeviceUpdateRequest(
    @field:Schema(nullable = true) val name: String? = null,
    @field:Schema(nullable = true) val assignedWorkerId: String? = null,
    @field:Schema(nullable = true) val status: DeviceStatus? = null,
    @field:Schema(nullable = true) val metadata: BandDeviceMetadata? = null,
)

@Schema(description = "스마트밴드 응답")
data class BandDeviceResponse(
    val bandId: String,
    val name: String,
    val assignedWorkerId: String?,
    val status: DeviceStatus,
    val metadata: BandDeviceMetadata?,
)
