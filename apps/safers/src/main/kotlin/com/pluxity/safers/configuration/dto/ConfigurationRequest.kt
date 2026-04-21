package com.pluxity.safers.configuration.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "설정 등록 요청")
data class ConfigurationRequest(
    @field:NotBlank(message = "설정 키는 필수입니다.")
    @field:Schema(description = "설정 키", example = "WEATHER_API")
    val key: String,
    @field:NotBlank(message = "설정 값은 필수입니다.")
    @field:Schema(description = "설정 값", example = "your-api-key")
    val value: String,
)

@Schema(description = "설정 수정 요청")
data class ConfigurationUpdateRequest(
    @field:NotBlank(message = "설정 값은 필수입니다.")
    @field:Schema(description = "설정 값", example = "your-api-key")
    val value: String,
)
