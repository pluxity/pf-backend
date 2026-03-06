package com.pluxity.yongin.weather.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId

fun dummyWeather(
    id: Long? = 1L,
    measuredAt: String = "2026-03-06 12:00:00",
    temperature: Double = 15.5,
    humidity: Double = 60.0,
    windSpeed: Double = 3.5,
    windDirection: String = "북서",
    rainfall: Int = 0,
    pm10: Int = 35,
    pm25: Int = 15,
    pm10Status: String = "보통",
    pm25Status: String = "좋음",
    noise: Double = 45.0,
): Weather =
    Weather(
        measuredAt = measuredAt,
        temperature = temperature,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        rainfall = rainfall,
        pm10 = pm10,
        pm25 = pm25,
        pm10Status = pm10Status,
        pm25Status = pm25Status,
        noise = noise,
    ).withId(id).withAudit()
