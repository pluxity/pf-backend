package com.pluxity.yongin.processstatus.dto

import com.pluxity.common.core.dto.PageSearchRequest
import java.time.LocalDate

fun dummyWorkTypeResponse(
    id: Long = 1L,
    name: String = "토공",
) = WorkTypeResponse(
    id = id,
    name = name,
)

fun dummyProcessStatusResponse(
    id: Long = 1L,
    workDate: LocalDate = LocalDate.of(2026, 1, 15),
    workType: WorkTypeResponse = dummyWorkTypeResponse(),
    plannedRate: Int = 100,
    actualRate: Int = 100,
    isActive: Boolean = false,
) = ProcessStatusResponse(
    id = id,
    workDate = workDate,
    workType = workType,
    plannedRate = plannedRate,
    actualRate = actualRate,
    isActive = isActive,
)

fun dummyProcessStatusImageResponse(fileId: Long? = 1L) =
    ProcessStatusImageResponse(
        fileId = fileId,
    )

fun dummyProcessStatusImageRequest(fileId: Long = 1L) =
    ProcessStatusImageRequest(
        fileId = fileId,
    )

fun dummyWorkTypeRequest(name: String = "토공") =
    WorkTypeRequest(
        name = name,
    )

fun dummyProcessStatusRequest(
    id: Long? = null,
    workDate: LocalDate = LocalDate.of(2026, 1, 15),
    workTypeId: Long = 1L,
    plannedRate: Int = 100,
    actualRate: Int = 100,
    isActive: Boolean = false,
) = ProcessStatusRequest(
    id = id,
    workDate = workDate,
    workTypeId = workTypeId,
    plannedRate = plannedRate,
    actualRate = actualRate,
    isActive = isActive,
)

fun dummyProcessStatusBulkRequest(
    upserts: List<ProcessStatusRequest> = emptyList(),
    deletedIds: List<Long> = emptyList(),
) = ProcessStatusBulkRequest(
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
