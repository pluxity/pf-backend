package com.pluxity.safers.weather.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import com.pluxity.safers.site.entity.Site
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "weathers",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_weather_fcst",
            columnNames = ["fcst_date", "fcst_time", "category", "site_id"],
        ),
    ],
)
class Weather(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: Site,
    @Column(name = "base_date", nullable = false)
    var baseDate: String,
    @Column(name = "base_time", nullable = false)
    var baseTime: String,
    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    var category: WeatherCategory,
    @Column(name = "fcst_date", nullable = false)
    var fcstDate: String,
    @Column(name = "fcst_time", nullable = false)
    var fcstTime: String,
    @Column(name = "fcst_value", nullable = false)
    var fcstValue: String,
) : IdentityIdEntity()
