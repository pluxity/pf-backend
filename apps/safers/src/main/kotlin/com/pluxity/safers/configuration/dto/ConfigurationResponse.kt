package com.pluxity.safers.configuration.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.core.response.toBaseResponse
import com.pluxity.safers.configuration.entity.Configuration
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "설정 응답")
data class ConfigurationResponse(
    @field:Schema(description = "설정 키")
    val key: String,
    @field:Schema(description = "설정 값")
    val value: String,
    @field:JsonUnwrapped
    val baseResponse: BaseResponse,
)

fun Configuration.toResponse(): ConfigurationResponse =
    ConfigurationResponse(
        key = key,
        value = value,
        baseResponse = toBaseResponse(),
    )
