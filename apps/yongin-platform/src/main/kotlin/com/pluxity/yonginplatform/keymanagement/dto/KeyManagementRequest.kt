package com.pluxity.yonginplatform.keymanagement.dto

import com.pluxity.yonginplatform.keymanagement.entity.KeyManagementType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(description = "주요관리사항 등록 요청")
data class KeyManagementRequest(
    @field:Schema(description = "타입", example = "QUALITY", required = true)
    val type: KeyManagementType,
    @field:Schema(description = "제목", example = "콘크리트 품질관리", required = true)
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:Schema(description = "공법특징", example = "고강도 콘크리트 적용")
    val methodFeature: String?,
    @field:Schema(description = "공법내용", example = "압축강도 40MPa 이상")
    val methodContent: String?,
    @field:Schema(description = "공법추진방향", example = "품질시험 강화")
    val methodDirection: String?,
    @field:Schema(description = "타입별 순서 (1~10)", example = "1", required = true)
    @field:Min(value = 1, message = "순서는 1 이상이어야 합니다")
    val displayOrder: Int,
    @field:Schema(description = "파일 ID", example = "1")
    val fileId: Long?,
)

@Schema(description = "주요관리사항 수정 요청")
data class KeyManagementUpdateRequest(
    @field:Schema(description = "타입", example = "QUALITY", required = true)
    val type: KeyManagementType,
    @field:Schema(description = "제목", example = "콘크리트 품질관리", required = true)
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:Schema(description = "공법특징", example = "고강도 콘크리트 적용")
    val methodFeature: String?,
    @field:Schema(description = "공법내용", example = "압축강도 40MPa 이상")
    val methodContent: String?,
    @field:Schema(description = "공법추진방향", example = "품질시험 강화")
    val methodDirection: String?,
    @field:Schema(description = "파일 ID", example = "1")
    val fileId: Long?,
)
