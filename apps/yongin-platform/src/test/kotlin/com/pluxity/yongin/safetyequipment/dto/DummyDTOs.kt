package com.pluxity.yongin.safetyequipment.dto

fun dummySafetyEquipmentRequest(
    name: String = "안전모",
    quantity: Int = 100,
): SafetyEquipmentRequest =
    SafetyEquipmentRequest(
        name = name,
        quantity = quantity,
    )
