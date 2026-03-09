package com.pluxity.safers.cctv.dto

import com.pluxity.common.core.response.BaseResponse
import com.pluxity.safers.site.dto.SiteResponse
import com.pluxity.safers.site.dto.dummySiteResponse

fun dummyCctvResponse(
    id: Long = 1L,
    site: SiteResponse =
        dummySiteResponse(
            constructionStartDate = null,
            constructionEndDate = null,
            description = null,
            address = null,
            baseUrl = "http://media-server:9997",
        ),
    streamName: String = "cam1",
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
): CctvResponse =
    CctvResponse(
        id = id,
        site = site,
        streamName = streamName,
        name = name,
        lon = lon,
        lat = lat,
        alt = alt,
        baseResponse = baseResponse,
    )

fun dummyCctvPlaybackResponse(pathName: String = "playback-pb_38ae550d"): CctvPlaybackResponse =
    CctvPlaybackResponse(
        pathName = pathName,
    )

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
