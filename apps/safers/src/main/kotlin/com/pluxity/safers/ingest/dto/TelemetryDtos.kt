package com.pluxity.safers.ingest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "정규화 telemetry envelope (수집모듈이 forward)")
data class TelemetryRequest(
    @field:NotNull
    @field:Schema(description = "센서 종류 디스크리미네이터", example = "GAS")
    val sourceType: SourceType,
    @field:NotBlank
    @field:Schema(description = "디바이스 식별자", example = "GAS-MH203-01")
    val sourceId: String,
    @field:NotNull
    @field:Schema(example = "2026-04-24T10:15:30")
    val timestamp: LocalDateTime,
    @field:NotBlank
    @field:Schema(description = "InfluxDB measurement 이름", example = "gas_reading")
    val measurement: String,
    @field:Schema(description = "InfluxDB tag (인덱싱)", example = "{\"site_id\":\"42\",\"device_id\":\"GAS-MH203-01\",\"gas\":\"H2S\"}")
    val tags: Map<String, String> = emptyMap(),
    @field:Schema(description = "InfluxDB field (값)", example = "{\"value\":3.1,\"unit\":\"PPM\",\"battery\":87}")
    val fields: Map<String, Any> = emptyMap(),
)

enum class SourceType { GAS, BAND, SOS }
