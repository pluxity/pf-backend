package com.pluxity.yongin.cctv.dto

import com.pluxity.common.core.response.BaseResponse
import java.time.LocalDateTime

fun dummyCctvResponse(
    id: Long = 1L,
    streamName: String = "CCTV-001",
    name: String = "1번 카메라",
    lon: Double? = 127.0,
    lat: Double? = 37.0,
    alt: Double? = 50.0,
    baseResponse: BaseResponse =
        BaseResponse(
            createdAt = "2026-01-01T00:00:00",
            createdBy = "system",
            updatedAt = "2026-01-01T00:00:00",
            updatedBy = "system",
        ),
) = CctvResponse(
    id = id,
    streamName = streamName,
    name = name,
    lon = lon,
    lat = lat,
    alt = alt,
    baseResponse = baseResponse,
)

fun dummyCctvUpdateRequest(
    name: String = "1번 카메라",
    lon: Double? = 127.0,
    lat: Double? = 37.0,
    alt: Double? = 50.0,
) = CctvUpdateRequest(
    name = name,
    lon = lon,
    lat = lat,
    alt = alt,
)

fun dummyCctvBookmarkResponse(
    id: Long = 1L,
    streamName: String = "CCTV-001",
    displayOrder: Int = 1,
    createdAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0),
    createdBy: String? = "system",
) = CctvBookmarkResponse(
    id = id,
    streamName = streamName,
    displayOrder = displayOrder,
    createdAt = createdAt,
    createdBy = createdBy,
)

fun dummyCctvBookmarkRequest(streamName: String = "CCTV-001") =
    CctvBookmarkRequest(
        streamName = streamName,
    )

fun dummyCctvBookmarkOrderRequest(ids: List<Long> = listOf(1L, 2L, 3L)) =
    CctvBookmarkOrderRequest(
        ids = ids,
    )
