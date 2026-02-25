package com.pluxity.yongin.weather.repository

import com.pluxity.yongin.weather.entity.Weather
import org.springframework.data.jpa.repository.JpaRepository

interface WeatherRepository : JpaRepository<Weather, Long> {
    fun findTopByOrderByIdDesc(): Weather?
}
