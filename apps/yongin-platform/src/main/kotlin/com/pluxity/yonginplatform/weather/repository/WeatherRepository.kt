package com.pluxity.yonginplatform.weather.repository

import com.pluxity.yonginplatform.weather.entity.Weather
import org.springframework.data.jpa.repository.JpaRepository

interface WeatherRepository : JpaRepository<Weather, Long> {
    fun findTopByOrderByIdDesc(): Weather?
}
