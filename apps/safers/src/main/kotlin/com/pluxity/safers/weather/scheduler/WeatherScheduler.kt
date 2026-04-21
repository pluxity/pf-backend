package com.pluxity.safers.weather.scheduler

import com.pluxity.safers.weather.service.WeatherFacade
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WeatherScheduler(
    private val weatherFacade: WeatherFacade,
) {
    @Scheduled(cron = "0 50 * * * *")
    fun collectForecast() {
        weatherFacade.collectForecast()
    }

    @Scheduled(cron = "0 15 * * * *")
    fun collectObservation() {
        weatherFacade.collectObservation()
    }
}
