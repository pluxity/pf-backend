package com.pluxity.yonginplatform.safetyequipment.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId

fun dummySafetyEquipment(
    id: Long? = 1L,
    name: String = "안전모",
    quantity: Int = 100,
): SafetyEquipment =
    SafetyEquipment(
        name = name,
        quantity = quantity,
    ).withId(id).withAudit()
