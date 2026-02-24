package com.pluxity.safers.weather.repository

import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.weather.entity.Weather
import com.pluxity.safers.weather.entity.WeatherCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WeatherRepository : JpaRepository<Weather, Long> {
    fun findByFcstDateAndFcstTimeAndCategoryAndSite(
        fcstDate: String,
        fcstTime: String,
        category: WeatherCategory,
        site: Site,
    ): Weather?

    @Query(
        """
        SELECT w FROM Weather w
        WHERE w.site.id = :siteId
        AND CONCAT(w.fcstDate, w.fcstTime) BETWEEN :startDateTime AND :endDateTime
        ORDER BY w.fcstDate, w.fcstTime, w.category
        """,
    )
    fun findBySiteIdAndFcstDateTimeBetween(
        siteId: Long,
        startDateTime: String,
        endDateTime: String,
    ): List<Weather>
}
