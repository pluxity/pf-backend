package com.pluxity.safers.weather.service

import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.weather.dto.WeatherApiResponse
import com.pluxity.safers.weather.dto.WeatherItemResponse
import com.pluxity.safers.weather.dto.WeatherTimeGroupResponse
import com.pluxity.safers.weather.entity.Weather
import com.pluxity.safers.weather.entity.WeatherCategory
import com.pluxity.safers.weather.repository.WeatherRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class WeatherService(
    private val weatherRepository: WeatherRepository,
) {
    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }

    fun findDashboard(siteId: Long): List<WeatherTimeGroupResponse> {
        val now = LocalDateTime.now()
        val startDateTime = now.minusHours(4).format(DATE_TIME_FORMATTER)
        val endDateTime = now.plusHours(5).format(DATE_TIME_FORMATTER)

        return weatherRepository
            .findBySiteIdAndFcstDateTimeBetween(siteId, startDateTime, endDateTime)
            .groupBy { it.fcstDate to it.fcstTime }
            .map { (key, weathers) ->
                WeatherTimeGroupResponse(
                    fcstDate = key.first,
                    fcstTime = key.second,
                    items =
                        weathers.map { weather ->
                            WeatherItemResponse(
                                category = weather.category,
                                value = weather.fcstValue,
                            )
                        },
                )
            }
    }

    @Transactional
    fun saveForecastData(results: List<Pair<Site, WeatherApiResponse>>) {
        results.forEach { (site, response) ->
            val items = extractItems(response) ?: return@forEach
            val fcstDates = items.mapNotNull { it.fcstDate }.toSet()
            val existingMap =
                weatherRepository
                    .findBySiteAndFcstDateIn(site, fcstDates)
                    .associateBy { Triple(it.fcstDate, it.fcstTime, it.category) }

            items.forEach { item ->
                val category = WeatherCategory.fromName(item.category) ?: return@forEach
                val fcstDate = item.fcstDate ?: return@forEach
                val fcstTime = item.fcstTime ?: return@forEach
                val fcstValue = item.fcstValue ?: return@forEach

                val existing = existingMap[Triple(fcstDate, fcstTime, category)]
                if (existing != null) {
                    existing.baseDate = item.baseDate
                    existing.baseTime = item.baseTime
                    existing.fcstValue = fcstValue
                } else {
                    weatherRepository.save(
                        Weather(
                            site = site,
                            baseDate = item.baseDate,
                            baseTime = item.baseTime,
                            category = category,
                            fcstDate = fcstDate,
                            fcstTime = fcstTime,
                            fcstValue = fcstValue,
                        ),
                    )
                }
            }

            log.info { "초단기 예보 수집 완료 - 현장: ${site.name}, nx: ${site.nx}, ny: ${site.ny}" }
        }
    }

    @Transactional
    fun saveObservationData(results: List<Pair<Site, WeatherApiResponse>>) {
        results.forEach { (site, response) ->
            val items = extractItems(response) ?: return@forEach
            val fcstDates = items.map { it.baseDate }.toSet()
            val existingMap =
                weatherRepository
                    .findBySiteAndFcstDateIn(site, fcstDates)
                    .associateBy { Triple(it.fcstDate, it.fcstTime, it.category) }

            items.forEach { item ->
                val category = WeatherCategory.fromName(item.category) ?: return@forEach
                val obsrValue = item.obsrValue ?: return@forEach

                val existing = existingMap[Triple(item.baseDate, item.baseTime, category)]
                if (existing != null) {
                    existing.baseDate = item.baseDate
                    existing.baseTime = item.baseTime
                    existing.fcstValue = obsrValue
                } else {
                    weatherRepository.save(
                        Weather(
                            site = site,
                            baseDate = item.baseDate,
                            baseTime = item.baseTime,
                            category = category,
                            fcstDate = item.baseDate,
                            fcstTime = item.baseTime,
                            fcstValue = obsrValue,
                        ),
                    )
                }
            }

            log.info { "초단기 실황 수집 완료 - 현장: ${site.name}, nx: ${site.nx}, ny: ${site.ny}" }
        }
    }

    private fun extractItems(response: WeatherApiResponse): List<WeatherApiResponse.Item>? {
        val header = response.response.header
        if (header.resultCode != "00") {
            log.error { "기상청 API 에러 - code: ${header.resultCode}, msg: ${header.resultMsg}" }
            return null
        }
        return response.response.body
            ?.items
            ?.item
    }
}
