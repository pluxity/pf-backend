package com.pluxity.safersCollect.v1.collect.dto

import com.pluxity.safersCollect.v1.collect.enums.GasType
import com.pluxity.safersCollect.v1.collect.enums.GasUnit
import com.pluxity.safersCollect.v1.collect.enums.WearState
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "기기 측정 raw 위치 (옵션)")
data class RawPosition(
    @field:Schema(example = "37.501234") val lat: Double,
    @field:Schema(example = "127.039876") val lon: Double,
    @field:Schema(description = "고도 (m, GPS 측정값)", example = "12.5", nullable = true) val alt: Double? = null,
    @field:Schema(description = "측위 정확도 (m)", example = "3.5", nullable = true) val accuracyM: Double? = null,
)

// ───────────────────────────────────── 가스 측정값 ─────────────────────────────────────

@Schema(description = "가스 측정값 수집 요청")
data class GasCollectRequest(
    @field:Valid
    @field:Size(min = 1, max = 500)
    val samples: List<GasSample>,
)

data class GasSample(
    @field:NotBlank @field:Schema(example = "GAS-MH203-01") val deviceId: String,
    @field:Schema(example = "2026-04-24T10:15:30") val timestamp: LocalDateTime,
    @field:Valid val measurements: List<GasMeasurement>,
    @field:Schema(description = "0~100", example = "87", nullable = true) val battery: Int? = null,
    @field:Schema(description = "신호 세기 (dBm)", example = "-68", nullable = true) val signalRssi: Int? = null,
)

data class GasMeasurement(
    val gas: GasType,
    @field:Schema(example = "3.1") val value: Double,
    val unit: GasUnit,
)

// ───────────────────────────────────── 스마트밴드 측정값 ─────────────────────────────────────

@Schema(description = "스마트밴드 측정값 수집 요청")
data class BandCollectRequest(
    @field:Valid
    @field:Size(min = 1, max = 500)
    val samples: List<BandSample>,
)

data class BandSample(
    @field:NotBlank @field:Schema(example = "BAND-A1B2C3") val bandId: String,
    @field:Schema(example = "2026-04-24T10:15:30") val timestamp: LocalDateTime,
    @field:Schema(nullable = true) val rawPosition: RawPosition? = null,
    @field:Schema(nullable = true) val vitals: BandVitals? = null,
    @field:Schema(description = "0~100", example = "73", nullable = true) val battery: Int? = null,
    @field:Schema(nullable = true) val wearState: WearState? = null,
)

data class BandVitals(
    @field:Schema(description = "심박수", example = "88", nullable = true) val heartRateBpm: Int? = null,
    @field:Schema(description = "체온 (℃)", example = "36.7", nullable = true) val bodyTempC: Double? = null,
    @field:Schema(description = "산소포화도 (%)", example = "97", nullable = true) val spo2: Int? = null,
    @field:Schema(example = "4321", nullable = true) val step: Int? = null,
)
