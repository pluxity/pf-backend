package com.pluxity.yonginplatform.keymanagement.dto

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.yonginplatform.keymanagement.entity.KeyManagement
import com.pluxity.yonginplatform.keymanagement.entity.KeyManagementType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주요관리사항 응답")
data class KeyManagementResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "타입", example = "QUALITY")
    val type: KeyManagementType,
    @field:Schema(description = "제목", example = "콘크리트 품질관리")
    val title: String,
    @field:Schema(description = "공법특징", example = "고강도 콘크리트 적용")
    val methodFeature: String?,
    @field:Schema(description = "공법내용", example = "압축강도 40MPa 이상")
    val methodContent: String?,
    @field:Schema(description = "공법추진방향", example = "품질시험 강화")
    val methodDirection: String?,
    @field:Schema(description = "순서", example = "1")
    val displayOrder: Int,
    @field:Schema(description = "파일 ID", example = "1")
    val fileId: Long?,
    @field:Schema(description = "파일 정보")
    val file: FileResponse?,
    @field:Schema(description = "선택여부", example = "true")
    val selected: Boolean,
)

fun KeyManagement.toResponse(file: FileResponse?): KeyManagementResponse =
    KeyManagementResponse(
        id = this.requiredId,
        type = this.type,
        title = this.title,
        methodFeature = this.methodFeature,
        methodContent = this.methodContent,
        methodDirection = this.methodDirection,
        displayOrder = this.displayOrder,
        fileId = this.fileId,
        file = file ?: FileResponse(),
        selected = this.selected,
    )
