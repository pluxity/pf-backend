package com.pluxity.safersCollect.v1.devices.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "SOS 디바이스 metadata")
data class SosDeviceMetadata(
    @field:Schema(example = "Acme", nullable = true) val vendor: String? = null,
    @field:Schema(example = "SOS-Pendant-1", nullable = true) val model: String? = null,
    @field:Schema(example = "1.0.0", nullable = true) val firmware: String? = null,
)

@Schema(description = "SOS 디바이스 등록 요청 (밴드와 별도 SOS 단말일 때만 사용)")
data class SosDeviceCreateRequest(
    @field:NotBlank @field:Schema(example = "SOS-X1Y2Z3") val deviceId: String,
    @field:NotBlank @field:Schema(example = "1조 SOS 펜던트") val name: String,
    @field:Schema(description = "소유 근로자 ID", example = "W102", nullable = true) val ownerWorkerId: String? = null,
    @field:Schema(nullable = true) val metadata: SosDeviceMetadata? = null,
)

@Schema(description = "SOS 디바이스 부분 수정 요청")
data class SosDeviceUpdateRequest(
    @field:Schema(nullable = true) val name: String? = null,
    @field:Schema(nullable = true) val ownerWorkerId: String? = null,
    @field:Schema(nullable = true) val status: DeviceStatus? = null,
    @field:Schema(nullable = true) val metadata: SosDeviceMetadata? = null,
)

@Schema(description = "SOS 디바이스 응답")
data class SosDeviceResponse(
    val deviceId: String,
    val name: String,
    val ownerWorkerId: String?,
    val status: DeviceStatus,
    val metadata: SosDeviceMetadata?,
)
