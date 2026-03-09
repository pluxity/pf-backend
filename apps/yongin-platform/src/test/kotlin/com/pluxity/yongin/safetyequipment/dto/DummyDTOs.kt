package com.pluxity.yongin.safetyequipment.dto

import com.pluxity.common.core.response.BaseResponse

fun dummySafetyEquipmentRequest(
    name: String = "안전모",
    quantity: Int = 100,
): SafetyEquipmentRequest =
    SafetyEquipmentRequest(
        name = name,
        quantity = quantity,
    )

fun dummySafetyEquipmentResponse(
    id: Long = 1L,
    name: String = "안전모",
    quantity: Int = 100,
    baseResponse: BaseResponse =
        BaseResponse(
            createdAt = "2026-01-01T00:00:00",
            createdBy = "system",
            updatedAt = "2026-01-01T00:00:00",
            updatedBy = "system",
        ),
) = SafetyEquipmentResponse(
    id = id,
    name = name,
    quantity = quantity,
    baseResponse = baseResponse,
)
