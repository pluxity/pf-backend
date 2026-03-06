package com.pluxity.safers.weather.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.site.entity.dummySite

fun dummyWeather(
    id: Long? = 1L,
    site: Site = dummySite(),
    baseDate: String = "20260306",
    baseTime: String = "1200",
    category: WeatherCategory = WeatherCategory.T1H,
    fcstDate: String = "20260306",
    fcstTime: String = "1300",
    fcstValue: String = "15.0",
): Weather =
    Weather(
        site = site,
        baseDate = baseDate,
        baseTime = baseTime,
        category = category,
        fcstDate = fcstDate,
        fcstTime = fcstTime,
        fcstValue = fcstValue,
    ).withId(id).withAudit()
