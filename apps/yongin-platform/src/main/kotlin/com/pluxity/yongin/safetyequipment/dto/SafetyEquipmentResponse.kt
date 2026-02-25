package com.pluxity.yongin.safetyequipment.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.core.response.toBaseResponse
import com.pluxity.yongin.safetyequipment.entity.SafetyEquipment
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "안전장비 응답")
data class SafetyEquipmentResponse(
    @field:Schema(description = "안전장비 ID")
    val id: Long,
    @field:Schema(description = "장비명")
    val name: String,
    @field:Schema(description = "개수")
    val quantity: Int,
    @field:JsonUnwrapped
    val baseResponse: BaseResponse,
)

fun SafetyEquipment.toResponse(): SafetyEquipmentResponse =
    SafetyEquipmentResponse(
        id = requiredId,
        name = name,
        quantity = quantity,
        baseResponse = toBaseResponse(),
    )
