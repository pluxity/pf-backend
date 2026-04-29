package com.pluxity.safersCollect.v1.devices.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

enum class DeviceStatus { ACTIVE, OFFLINE, RETIRED }

@Schema(description = "3D 모델 로컬 좌표")
data class InstallLocal(
    @field:Schema(example = "12.5") val x: Double,
    @field:Schema(example = "4.2") val y: Double,
    @field:Schema(example = "-3.0") val z: Double,
)

@Schema(description = "가스센서 디바이스 metadata (type 별 자유 필드)")
data class GasDeviceMetadata(
    @field:Schema(example = "Acme", nullable = true) val vendor: String? = null,
    @field:Schema(example = "GX-9", nullable = true) val model: String? = null,
    @field:Schema(example = "2026-03-01", nullable = true) val calibratedAt: String? = null,
)

@Schema(description = "가스센서 등록 요청")
data class GasDeviceCreateRequest(
    @field:NotBlank @field:Schema(example = "GAS-MH203-01") val deviceId: String,
    @field:NotBlank @field:Schema(example = "맨홀 203 가스센서") val name: String,
    @field:NotBlank @field:Schema(example = "MH-203") val facilityId: String,
    val installLocal: InstallLocal,
    @field:Schema(example = "B1", nullable = true) val floor: String? = null,
    @field:Schema(nullable = true) val metadata: GasDeviceMetadata? = null,
)

@Schema(description = "가스센서 부분 수정 요청")
data class GasDeviceUpdateRequest(
    @field:Schema(nullable = true) val name: String? = null,
    @field:Schema(nullable = true) val facilityId: String? = null,
    @field:Schema(nullable = true) val installLocal: InstallLocal? = null,
    @field:Schema(nullable = true) val floor: String? = null,
    @field:Schema(nullable = true) val status: DeviceStatus? = null,
    @field:Schema(nullable = true) val metadata: GasDeviceMetadata? = null,
)

@Schema(description = "가스센서 응답")
data class GasDeviceResponse(
    val deviceId: String,
    val name: String,
    val facilityId: String,
    val installLocal: InstallLocal,
    val floor: String?,
    val status: DeviceStatus,
    val metadata: GasDeviceMetadata?,
)
