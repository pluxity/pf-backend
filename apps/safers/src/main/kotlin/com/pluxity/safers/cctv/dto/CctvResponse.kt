package com.pluxity.safers.cctv.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.common.core.response.BaseResponse
import com.pluxity.common.core.response.toBaseResponse
import com.pluxity.safers.cctv.entity.Cctv
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CCTV 응답")
data class CctvResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long,
    @field:Schema(description = "스트림명", example = "cam1")
    val streamName: String,
    @field:Schema(description = "이름", example = "1번 카메라")
    val name: String,
    @field:Schema(description = "경도", example = "127.0")
    val lon: Double?,
    @field:Schema(description = "위도", example = "37.0")
    val lat: Double?,
    @field:Schema(description = "고도", example = "50.0")
    val alt: Double?,
    @field:JsonUnwrapped
    val baseResponse: BaseResponse,
)

fun Cctv.toResponse(): CctvResponse =
    CctvResponse(
        id = this.requiredId,
        streamName = this.streamName,
        name = this.name,
        lon = this.lon,
        lat = this.lat,
        alt = this.alt,
        baseResponse = this.toBaseResponse(),
    )
