package com.pluxity.yonginplatform.weather.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.yonginplatform.weather.dto.WeatherDataDto
import com.pluxity.yonginplatform.weather.dto.WeatherResponse
import com.pluxity.yonginplatform.weather.dto.toResponse
import com.pluxity.yonginplatform.weather.entity.Weather
import com.pluxity.yonginplatform.weather.repository.WeatherRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class WeatherService(
    private val repository: WeatherRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun saveFromJson(jsonData: String) {
        val weatherData = objectMapper.readValue(jsonData, WeatherDataDto::class.java)
        log.info { weatherData }
        repository.save(
            Weather(
                measuredAt = weatherData.measuredAt,
                temperature = weatherData.temperature,
                humidity = weatherData.humidity,
                windSpeed = weatherData.windSpeed,
                windDirection = weatherData.windDirection,
                rainfall = weatherData.rainfall,
                pm10 = weatherData.pm10,
                pm25 = weatherData.pm25,
                pm10Status = weatherData.pm10Status,
                pm25Status = weatherData.pm25Status,
                noise = weatherData.noise,
            ),
        )
    }

    fun findLatest(): WeatherResponse = repository.findTopByOrderByIdDesc()?.toResponse() ?: WeatherResponse()
}
