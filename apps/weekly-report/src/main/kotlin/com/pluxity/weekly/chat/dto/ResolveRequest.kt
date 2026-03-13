package com.pluxity.weekly.chat.dto

import jakarta.validation.constraints.NotEmpty

data class ResolveRequest(
    @field:NotEmpty(message = "partial은 필수입니다")
    val partial: Map<String, Any?>,
    @field:NotEmpty(message = "선택된 항목은 필수입니다")
    val selected: Map<String, String>,
)
