package com.pluxity.yongin.weather.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "weather")
class Weather(
    @Column(name = "measured_at", nullable = false)
    var measuredAt: String,
    @Column(name = "temperature", nullable = false)
    var temperature: Double,
    @Column(name = "humidity", nullable = false)
    var humidity: Double,
    @Column(name = "wind_speed", nullable = false)
    var windSpeed: Double,
    @Column(name = "wind_direction", nullable = false)
    var windDirection: String,
    @Column(name = "rainfall", nullable = false)
    var rainfall: Int,
    @Column(name = "pm10", nullable = false)
    var pm10: Int,
    @Column(name = "pm25", nullable = false)
    var pm25: Int,
    @Column(name = "pm10_status", nullable = false)
    var pm10Status: String,
    @Column(name = "pm25_status", nullable = false)
    var pm25Status: String,
    @Column(name = "noise", nullable = false)
    var noise: Double,
) : IdentityIdEntity()
