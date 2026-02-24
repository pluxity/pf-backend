package com.pluxity.safers.weather.scheduler

import com.pluxity.safers.weather.service.WeatherService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WeatherScheduler(
    private val weatherService: WeatherService,
) {
    @Scheduled(cron = "0 45 * * * *")
    fun collectForecast() {
        weatherService.collectForecast()
    }

    @Scheduled(cron = "0 10 * * * *")
    fun collectObservation() {
        weatherService.collectObservation()
    }
}
