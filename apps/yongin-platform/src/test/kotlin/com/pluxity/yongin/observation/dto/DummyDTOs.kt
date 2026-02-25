package com.pluxity.yongin.observation.dto

import java.time.LocalDate

fun dummyObservationRequest(
    date: LocalDate = LocalDate.now(),
    description: String? = "테스트 설명",
    fileId: Long = 1L,
    rootFileName: String = "test_file",
) = ObservationRequest(
    date = date,
    description = description,
    fileId = fileId,
    rootFileName = rootFileName,
)
