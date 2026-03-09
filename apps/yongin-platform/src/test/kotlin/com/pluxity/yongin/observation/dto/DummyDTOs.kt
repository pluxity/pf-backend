package com.pluxity.yongin.observation.dto

import java.time.LocalDate

fun dummyObservationRequest(
    date: LocalDate = LocalDate.of(2026, 1, 15),
    description: String? = "테스트 설명",
    fileId: Long = 1L,
    rootFileName: String = "test-model",
) = ObservationRequest(
    date = date,
    description = description,
    fileId = fileId,
    rootFileName = rootFileName,
)

fun dummyObservationResponse(
    id: Long = 1L,
    date: LocalDate = LocalDate.of(2026, 1, 15),
    description: String? = "테스트 설명",
    fileId: Long = 1L,
    rootFileName: String = "test-model",
    filePath: String? = "/files/test-model",
) = ObservationResponse(
    id = id,
    date = date,
    description = description,
    fileId = fileId,
    rootFileName = rootFileName,
    filePath = filePath,
)
