package com.pluxity.weekly.chat.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "선택 후보")
data class Candidate(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "표시명", example = "알파 프로젝트")
    val name: String,
)
