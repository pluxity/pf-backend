package com.pluxity.common.file.dto

import com.pluxity.common.file.entity.ZipContentEntry
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ZIP 파일 내 항목 정보")
data class ZipEntryInfo(
    @field:Schema(description = "항목 이름", example = "tileset.json")
    val name: String,
    @field:Schema(description = "디렉토리 여부", example = "false")
    val isDirectory: Boolean,
)

fun ZipContentEntry.toZipEntryInfo() =
    ZipEntryInfo(
        name = this.name,
        isDirectory = this.isDirectory,
    )
