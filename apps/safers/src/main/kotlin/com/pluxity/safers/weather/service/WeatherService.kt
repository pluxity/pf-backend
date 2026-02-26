package com.pluxity.safers.weather.service

import com.pluxity.safers.site.repository.SiteRepository
import com.pluxity.safers.weather.client.WeatherApiClient
import com.pluxity.safers.weather.dto.WeatherApiResponse
import com.pluxity.safers.weather.dto.WeatherItemResponse
import com.pluxity.safers.weather.dto.WeatherTimeGroupResponse
import com.pluxity.safers.weather.entity.Weather
import com.pluxity.safers.weather.entity.WeatherCategory
import com.pluxity.safers.weather.repository.WeatherRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Service
class WeatherService(
    private val weatherRepository: WeatherRepository,
    private val weatherApiClient: WeatherApiClient,
    private val siteRepository: SiteRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH")
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }

    @Transactional(readOnly = true)
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

    fun collectForecast() {
        val sites = siteRepository.findAll().filter { it.nx != 0 }
        if (sites.isEmpty()) {
            log.info { "등록된 현장이 없어 초단기 예보 수집을 건너뜁니다." }
            return
        }

        val now = LocalDateTime.now()
        val baseDate = now.format(DATE_FORMATTER)
        val baseTime = now.format(HOUR_FORMATTER) + "30"

        log.info { "초단기 예보 수집 시작 - baseDate: $baseDate, baseTime: $baseTime, 현장 수: ${sites.size}" }

        val results =
            Flux
                .fromIterable(sites)
                .flatMap({ site ->
                    weatherApiClient
                        .fetchUltraSrtFcst(baseDate, baseTime, site.nx, site.ny)
                        .map { site to it }
                }, 4)
                .collectList()
                .block() ?: return

        transactionTemplate.execute {
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
    }

    fun collectObservation() {
        val sites = siteRepository.findAll().filter { it.nx != 0 }
        if (sites.isEmpty()) {
            log.info { "등록된 현장이 없어 초단기 실황 수집을 건너뜁니다." }
            return
        }

        val now = LocalDateTime.now()
        val baseDate = now.format(DATE_FORMATTER)
        val baseTime = now.format(HOUR_FORMATTER) + "00"

        log.info { "초단기 실황 수집 시작 - baseDate: $baseDate, baseTime: $baseTime, 현장 수: ${sites.size}" }

        val results =
            Flux
                .fromIterable(sites)
                .flatMap({ site ->
                    weatherApiClient
                        .fetchUltraSrtNcst(baseDate, baseTime, site.nx, site.ny)
                        .map { site to it }
                }, 4)
                .collectList()
                .block() ?: return

        transactionTemplate.execute {
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
