package com.pluxity.safers.site.dto

import com.pluxity.safers.site.entity.Region
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

@Schema(description = "현장 요청")
data class SiteRequest(
    @field:NotBlank(message = "현장명은 필수입니다.")
    @field:Schema(description = "현장명", example = "서울역 현장")
    val name: String,
    @field:Schema(description = "공사 시작일", example = "2024-01-01")
    val constructionStartDate: LocalDate? = null,
    @field:Schema(description = "공사 종료일", example = "2025-12-31")
    val constructionEndDate: LocalDate? = null,
    @field:Schema(description = "현장 설명", example = "서울역 리모델링 현장")
    val description: String? = null,
    @field:Schema(description = "지역", example = "SEOUL")
    val region: Region? = null,
    @field:Schema(description = "주소", example = "서울특별시 용산구 한강대로 405")
    val address: String? = null,
    @field:Schema(description = "Base URL", example = "https://example.com/api")
    val baseUrl: String? = null,
    @field:NotBlank(message = "위치 정보는 필수입니다.")
    @field:Schema(
        description = "위치 (WKT 형식)",
        example = "POLYGON ((126.96 37.55, 126.98 37.55, 126.98 37.56, 126.96 37.56, 126.96 37.55))",
    )
    val location: String,
    @field:Schema(description = "썸네일 이미지 파일 ID", example = "1")
    val thumbnailImageId: Long? = null,
)
