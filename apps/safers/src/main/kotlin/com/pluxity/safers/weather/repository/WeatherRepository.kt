package com.pluxity.safers.weather.repository

import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.weather.entity.Weather
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WeatherRepository : JpaRepository<Weather, Long> {
    fun findBySiteAndFcstDateIn(
        site: Site,
        fcstDates: Set<String>,
    ): List<Weather>

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
