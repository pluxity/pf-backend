package com.pluxity.safers.site.dto

import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.file.dto.FileResponse
import com.pluxity.safers.site.entity.Region
import java.time.LocalDate

fun dummySiteResponse(
    id: Long = 1L,
    name: String = "서울역 현장",
    constructionStartDate: LocalDate? = LocalDate.of(2026, 1, 1),
    constructionEndDate: LocalDate? = LocalDate.of(2026, 12, 31),
    description: String? = "서울역 리모델링 현장",
    region: Region? = Region.SEOUL,
    address: String? = "서울특별시 용산구 한강대로 405",
    baseUrl: String? = "https://example.com/api",
    location: String = "POLYGON ((126.96 37.55, 126.98 37.55, 126.98 37.56, 126.96 37.56, 126.96 37.55))",
    thumbnailImage: FileResponse? = null,
    baseResponse: BaseResponse =
        BaseResponse(
            createdAt = "2026-01-01T00:00:00",
            createdBy = "system",
            updatedAt = "2026-01-01T00:00:00",
            updatedBy = "system",
        ),
): SiteResponse =
    SiteResponse(
        id = id,
        name = name,
        constructionStartDate = constructionStartDate,
        constructionEndDate = constructionEndDate,
        description = description,
        region = region,
        address = address,
        baseUrl = baseUrl,
        location = location,
        thumbnailImage = thumbnailImage,
        baseResponse = baseResponse,
    )

fun dummySiteRequest(
    name: String = "서울역 현장",
    constructionStartDate: LocalDate? = LocalDate.of(2026, 1, 1),
    constructionEndDate: LocalDate? = LocalDate.of(2026, 12, 31),
    description: String? = "서울역 리모델링 현장",
    region: Region? = Region.SEOUL,
    address: String? = "서울특별시 용산구 한강대로 405",
    location: String = "POLYGON ((126.96 37.55, 126.98 37.55, 126.98 37.56, 126.96 37.56, 126.96 37.55))",
    thumbnailImageId: Long? = null,
    baseUrl: String? = "https://example.com/api",
): SiteRequest =
    SiteRequest(
        name = name,
        constructionStartDate = constructionStartDate,
        constructionEndDate = constructionEndDate,
        description = description,
        region = region,
        address = address,
        location = location,
        thumbnailImageId = thumbnailImageId,
        baseUrl = baseUrl,
    )
