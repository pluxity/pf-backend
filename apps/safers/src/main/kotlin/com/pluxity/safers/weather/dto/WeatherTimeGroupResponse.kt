package com.pluxity.safers.weather.dto

import com.pluxity.safers.weather.entity.WeatherCategory
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "시간별 날씨 데이터")
data class WeatherTimeGroupResponse(
    @field:Schema(description = "일자 (yyyyMMdd)")
    val fcstDate: String,
    @field:Schema(description = "시각 (HHmm)")
    val fcstTime: String,
    @field:Schema(description = "해당 시간의 날씨 항목 목록")
    val items: List<WeatherItemResponse>,
)

@Schema(description = "날씨 항목")
data class WeatherItemResponse(
    @field:Schema(description = "카테고리")
    val category: WeatherCategory,
    @field:Schema(description = "값")
    val value: String,
)
