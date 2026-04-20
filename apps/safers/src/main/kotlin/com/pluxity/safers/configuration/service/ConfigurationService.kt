package com.pluxity.safers.configuration.service

import com.pluxity.safers.configuration.repository.ConfigurationRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ConfigurationService(
    private val configurationRepository: ConfigurationRepository,
) {
    fun findValue(key: String): String? = configurationRepository.findByKey(key)?.value

    @Cacheable(value = [WEATHER_API_KEY_CACHE], unless = "#result == null")
    fun findWeatherApiKey(): String? = configurationRepository.findByKey(WEATHER_API_KEY)?.value

    companion object {
        const val WEATHER_API_KEY_CACHE = "weather-api-key"
        private const val WEATHER_API_KEY = "WEATHER_API"
    }
}
