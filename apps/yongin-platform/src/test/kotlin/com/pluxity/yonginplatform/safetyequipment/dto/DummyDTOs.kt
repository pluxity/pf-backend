package com.pluxity.yonginplatform.safetyequipment.dto

fun dummySafetyEquipmentRequest(
    name: String = "안전모",
    quantity: Int = 100,
): SafetyEquipmentRequest =
    SafetyEquipmentRequest(
        name = name,
        quantity = quantity,
    )
