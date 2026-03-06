package com.pluxity.safers.cctv.dto

fun dummyCctvUpdateRequest(
    name: String = "1번 카메라",
    lon: Double? = 127.0,
    lat: Double? = 37.0,
    alt: Double? = 50.0,
): CctvUpdateRequest =
    CctvUpdateRequest(
        name = name,
        lon = lon,
        lat = lat,
        alt = alt,
    )

fun dummyCctvPlaybackRequest(
    startDate: String = "20260304130000",
    endDate: String = "20260304140000",
): CctvPlaybackRequest =
    CctvPlaybackRequest(
        startDate = startDate,
        endDate = endDate,
    )
