package com.pluxity.safers.weather.service

import com.pluxity.safers.site.repository.SiteRepository
import com.pluxity.safers.weather.client.WeatherApiClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Component
class WeatherFacade(
    private val weatherService: WeatherService,
    private val weatherApiClient: WeatherApiClient,
    private val siteRepository: SiteRepository,
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH")
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

        weatherService.saveForecastData(results)
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

        weatherService.saveObservationData(results)
    }
}
