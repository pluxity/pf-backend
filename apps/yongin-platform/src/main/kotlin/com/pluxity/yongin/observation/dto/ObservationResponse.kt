package com.pluxity.yongin.observation.dto

import com.pluxity.yongin.observation.entity.Observation
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "드론 관측 데이터 응답")
data class ObservationResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "날짜", example = "2026-01-27")
    val date: LocalDate,
    @field:Schema(description = "설명", example = "드론 관측 데이터")
    val description: String?,
    @field:Schema(description = "파일 ID", example = "1")
    val fileId: Long,
    @field:Schema(description = "루트 파일명", example = "tileset.json")
    val rootFileName: String,
    @field:Schema(description = "파일 경로", example = "https://../tileset.json")
    val filePath: String?,
)

fun Observation.toResponse(baseUrl: String): ObservationResponse =
    ObservationResponse(
        id = this.requiredId,
        date = this.date,
        description = this.description,
        fileId = this.fileId,
        rootFileName = this.rootFileName,
        filePath = this.directoryPath?.let { "$baseUrl/$it/${this.rootFileName}" },
    )
