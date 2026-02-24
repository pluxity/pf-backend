package com.pluxity.safers.site.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.core.response.toBaseResponse
import com.pluxity.common.file.dto.FileResponse
import com.pluxity.safers.site.entity.Region
import com.pluxity.safers.site.entity.Site
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "현장 응답")
data class SiteResponse(
    @field:Schema(description = "현장 ID")
    val id: Long,
    @field:Schema(description = "현장명")
    val name: String,
    @field:Schema(description = "공사 시작일")
    val constructionStartDate: LocalDate?,
    @field:Schema(description = "공사 종료일")
    val constructionEndDate: LocalDate?,
    @field:Schema(description = "현장 설명")
    val description: String?,
    @field:Schema(description = "지역")
    val region: Region?,
    @field:Schema(description = "주소")
    val address: String?,
    @field:Schema(description = "위치 (WKT 형식)")
    val location: String,
    @field:Schema(description = "썸네일 이미지")
    val thumbnailImage: FileResponse?,
    @field:JsonUnwrapped
    val baseResponse: BaseResponse,
)

fun Site.toResponse(thumbnailFileResponse: FileResponse?): SiteResponse =
    SiteResponse(
        id = requiredId,
        name = name,
        constructionStartDate = constructionStartDate,
        constructionEndDate = constructionEndDate,
        description = description,
        region = region,
        address = address,
        location = location.toText(),
        thumbnailImage = thumbnailFileResponse,
        baseResponse = toBaseResponse(),
    )
