package com.pluxity.yongin.goal.entity

import com.pluxity.common.core.test.withId
import java.time.LocalDate

fun dummyConstructionSection(
    id: Long? = null,
    name: String = "절토",
) = ConstructionSection(
    name = name,
).withId(id)

fun dummyGoal(
    id: Long? = null,
    inputDate: LocalDate = LocalDate.of(2026, 1, 15),
    constructionSection: ConstructionSection = dummyConstructionSection(id = 1L),
    progressRate: Float = 75.5f,
    constructionRate: Float = 80.0f,
    totalQuantity: Int = 1000,
    cumulativeQuantity: Int = 755,
    previousCumulativeQuantity: Int = 700,
    targetQuantity: Int = 800,
    workQuantity: Int = 50,
    startDate: LocalDate = LocalDate.of(2026, 1, 1),
    completionDate: LocalDate = LocalDate.of(2026, 12, 31),
    plannedWorkDays: Int = 365,
    delayDays: Int = 0,
    isActive: Boolean = false,
) = Goal(
    inputDate = inputDate,
    constructionSection = constructionSection,
    progressRate = progressRate,
    constructionRate = constructionRate,
    totalQuantity = totalQuantity,
    cumulativeQuantity = cumulativeQuantity,
    previousCumulativeQuantity = previousCumulativeQuantity,
    targetQuantity = targetQuantity,
    workQuantity = workQuantity,
    startDate = startDate,
    completionDate = completionDate,
    plannedWorkDays = plannedWorkDays,
    delayDays = delayDays,
    isActive = isActive,
).withId(id)
