package com.pluxity.yongin.goal.dto

import com.pluxity.common.core.dto.PageSearchRequest
import java.time.LocalDate

fun dummyConstructionSectionResponse(
    id: Long = 1L,
    name: String = "절토",
) = ConstructionSectionResponse(
    id = id,
    name = name,
)

fun dummyGoalResponse(
    id: Long = 1L,
    inputDate: LocalDate = LocalDate.of(2026, 1, 15),
    constructionSection: ConstructionSectionResponse = dummyConstructionSectionResponse(),
    progressRate: Float = 75.5f,
    constructionRate: Float = 80.0f,
    totalQuantity: Int = 1000,
    cumulativeQuantity: Int = 800,
    previousCumulativeQuantity: Int = 750,
    targetQuantity: Int = 100,
    workQuantity: Int = 50,
    startDate: LocalDate = LocalDate.of(2025, 1, 1),
    completionDate: LocalDate = LocalDate.of(2026, 12, 31),
    plannedWorkDays: Int = 365,
    delayDays: Int = 0,
    isActive: Boolean = false,
) = GoalResponse(
    id = id,
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
)

fun dummyConstructionSectionRequest(name: String = "절토") =
    ConstructionSectionRequest(
        name = name,
    )

fun dummyGoalRequest(
    id: Long? = null,
    inputDate: LocalDate = LocalDate.of(2026, 1, 15),
    constructionSectionId: Long = 1L,
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
) = GoalRequest(
    id = id,
    inputDate = inputDate,
    constructionSectionId = constructionSectionId,
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
)

fun dummyGoalBulkRequest(
    upserts: List<GoalRequest> = emptyList(),
    deletedIds: List<Long> = emptyList(),
) = GoalBulkRequest(
    upserts = upserts,
    deletedIds = deletedIds,
)

fun dummyPageSearchRequest(
    page: Int = 1,
    size: Int = 10,
) = PageSearchRequest(
    page = page,
    size = size,
)
