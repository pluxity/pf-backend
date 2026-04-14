package com.pluxity.safers.chat.dto

import com.pluxity.safers.weather.dto.WeatherTimeGroupResponse

data class ChatWeatherResponse(
    val siteName: String,
    val forecasts: List<WeatherTimeGroupResponse>,
)
