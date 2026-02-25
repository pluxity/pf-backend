package com.pluxity.yongin.processstatus.entity

import com.pluxity.common.core.test.withId
import java.time.LocalDate

fun dummyWorkType(
    id: Long? = null,
    name: String = "토공",
) = WorkType(
    name = name,
).withId(id)

fun dummyProcessStatus(
    id: Long? = null,
    workDate: LocalDate = LocalDate.of(2026, 1, 15),
    workType: WorkType = dummyWorkType(id = 1L),
    plannedRate: Int = 100,
    actualRate: Int = 100,
    isActive: Boolean = false,
) = ProcessStatus(
    workDate = workDate,
    workType = workType,
    plannedRate = plannedRate,
    actualRate = actualRate,
    isActive = isActive,
).withId(id)
