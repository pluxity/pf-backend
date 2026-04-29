package com.pluxity.safersCollect.v1.collect.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

enum class EventSeverity { INFO, WARNING, CRITICAL }

enum class ThresholdLevel { WARNING, DANGER }

enum class SosTrigger { BUTTON_LONG_PRESS, MOBILE_APP, MANUAL_DISPATCH }

enum class VitalMetric { HEART_RATE, BODY_TEMP, SPO2 }

// ───────────────────────────────────── 가스 임계 초과 이벤트 ─────────────────────────────────────

@Schema(description = "가스 임계 초과 이벤트")
data class GasEventRequest(
    @field:NotBlank @field:Schema(example = "GAS-EVT-20260424-0001") val eventId: String,
    @field:NotNull val severity: EventSeverity,
    @field:NotNull @field:Schema(example = "2026-04-24T10:15:30") val occurredAt: LocalDateTime,
    @field:NotNull val payload: GasEventPayload,
)

data class GasEventPayload(
    @field:NotBlank @field:Schema(example = "GAS-MH203-01") val deviceId: String,
    @field:NotNull val triggered: List<GasTriggered>,
)

data class GasTriggered(
    @field:NotNull val gas: GasType,
    @field:NotNull val value: Double,
    @field:NotNull val unit: GasUnit,
    @field:NotNull val threshold: Double,
    @field:NotNull val level: ThresholdLevel,
)

// ───────────────────────────────────── SOS 이벤트 ─────────────────────────────────────

@Schema(description = "SOS 긴급호출 이벤트")
data class SosEventRequest(
    @field:NotBlank @field:Schema(example = "SOS-EVT-20260424-0001") val eventId: String,
    @field:NotNull val severity: EventSeverity,
    @field:NotNull @field:Schema(example = "2026-04-24T10:15:30") val occurredAt: LocalDateTime,
    @field:Schema(nullable = true) val rawPosition: RawPosition? = null,
    @field:NotNull val payload: SosEventPayload,
)

data class SosEventPayload(
    @field:NotBlank @field:Schema(example = "BAND-A1B2C3") val bandId: String,
    @field:NotNull val trigger: SosTrigger,
    @field:Schema(nullable = true) val vitals: BandVitals? = null,
    @field:Schema(nullable = true) val memo: String? = null,
)

// ───────────────────────────────────── 스마트밴드 이상 이벤트 ─────────────────────────────────────

enum class BandEventType {
    BAND_VITAL_ABNORMAL,
    BAND_FALL_DETECTED,
    BAND_OFFLINE,
}

@Schema(description = "스마트밴드 이상 이벤트 (eventType 으로 분기)")
data class BandEventRequest(
    @field:NotBlank @field:Schema(example = "BAND-EVT-20260424-0001") val eventId: String,
    @field:NotNull val eventType: BandEventType,
    @field:NotNull val severity: EventSeverity,
    @field:NotNull @field:Schema(example = "2026-04-24T10:15:30") val occurredAt: LocalDateTime,
    @field:Schema(nullable = true) val rawPosition: RawPosition? = null,
    @field:NotNull val payload: BandEventPayload,
)

@Schema(description = "BAND_VITAL_ABNORMAL: abnormal[] / BAND_FALL_DETECTED: impactG / BAND_OFFLINE: lastSeenAt")
data class BandEventPayload(
    @field:NotBlank @field:Schema(example = "BAND-A1B2C3") val bandId: String,
    @field:Schema(nullable = true) val abnormal: List<BandAbnormal>? = null,
    @field:Schema(example = "4.2", nullable = true) val impactG: Double? = null,
    @field:Schema(nullable = true) val lastSeenAt: LocalDateTime? = null,
)

data class BandAbnormal(
    @field:NotNull val metric: VitalMetric,
    @field:NotNull val value: Double,
    @field:NotBlank @field:Schema(example = "BPM") val unit: String,
    @field:Schema(nullable = true) val thresholdHigh: Double? = null,
    @field:Schema(nullable = true) val thresholdLow: Double? = null,
)
