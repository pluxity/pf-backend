package com.pluxity.yonginplatform.processstatus.dto

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.yonginplatform.processstatus.entity.ProcessStatusImage
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "공정관련 이미지 응답")
data class ProcessStatusImageResponse(
    @field:Schema(description = "파일 ID", example = "1")
    val fileId: Long? = null,
    @field:Schema(description = "파일 정보")
    val file: FileResponse? = FileResponse(),
    @field:Schema(description = "수정일시")
    val updatedAt: LocalDateTime? = null,
)

fun ProcessStatusImage.toResponse(file: FileResponse?): ProcessStatusImageResponse =
    ProcessStatusImageResponse(
        fileId = this.fileId,
        file = file,
        updatedAt = this.updatedAt,
    )
