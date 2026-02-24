package com.pluxity.yonginplatform.safetyequipment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "안전장비 요청")
data class SafetyEquipmentRequest(
    @field:NotBlank(message = "장비명은 필수입니다.")
    @field:Schema(description = "장비명", example = "안전모")
    val name: String,
    @field:Schema(description = "개수", example = "100")
    val quantity: Int,
)
