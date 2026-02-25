package com.pluxity.yongin.weather.dto

import com.pluxity.yongin.weather.entity.Weather
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "날씨 정보 응답")
data class WeatherResponse(
    @field:Schema(description = "ID", example = "1")
    val id: Long? = null,
    @field:Schema(description = "검침시간", example = "2024-01-15 12:00:00")
    val measuredAt: String? = null,
    @field:Schema(description = "온도", example = "25.5")
    val temperature: Double? = null,
    @field:Schema(description = "습도", example = "60.0")
    val humidity: Double? = null,
    @field:Schema(description = "풍속", example = "3.5")
    val windSpeed: Double? = null,
    @field:Schema(description = "풍향", example = "북서")
    val windDirection: String? = null,
    @field:Schema(description = "강우량", example = "0")
    val rainfall: Int? = null,
    @field:Schema(description = "미세먼지", example = "35")
    val pm10: Int? = null,
    @field:Schema(description = "초미세먼지", example = "15")
    val pm25: Int? = null,
    @field:Schema(description = "미세먼지상태", example = "보통")
    val pm10Status: String? = null,
    @field:Schema(description = "초미세먼지상태", example = "좋음")
    val pm25Status: String? = null,
    @field:Schema(description = "소음", example = "45.0")
    val noise: Double? = null,
)

fun Weather.toResponse(): WeatherResponse =
    WeatherResponse(
        id = this.requiredId,
        measuredAt = this.measuredAt,
        temperature = this.temperature,
        humidity = this.humidity,
        windSpeed = this.windSpeed,
        windDirection = this.windDirection,
        rainfall = this.rainfall,
        pm10 = this.pm10,
        pm25 = this.pm25,
        pm10Status = this.pm10Status,
        pm25Status = this.pm25Status,
        noise = this.noise,
    )
