package com.pluxity.safers.weather.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.configuration.repository.ConfigurationRepository
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.weather.dto.WeatherApiResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Component
class WeatherApiClient(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    webClientFactory: WebClientFactory,
    private val configurationRepository: ConfigurationRepository,
) {
    private val webClient = webClientFactory.createClient("https://apihub.kma.go.kr")

    companion object {
        private const val WEATHER_API_KEY = "WEATHER_API"
    }

    fun fetchUltraSrtFcst(
        baseDate: String,
        baseTime: String,
        nx: Int,
        ny: Int,
    ): Mono<WeatherApiResponse> = fetchWeather("getUltraSrtFcst", "초단기 예보", baseDate, baseTime, nx, ny)

    fun fetchUltraSrtNcst(
        baseDate: String,
        baseTime: String,
        nx: Int,
        ny: Int,
    ): Mono<WeatherApiResponse> = fetchWeather("getUltraSrtNcst", "초단기 실황", baseDate, baseTime, nx, ny)

    private fun fetchWeather(
        endpoint: String,
        label: String,
        baseDate: String,
        baseTime: String,
        nx: Int,
        ny: Int,
    ): Mono<WeatherApiResponse> {
        val authKey = getAuthKey()
        return webClient
            .get()
            .uri { builder ->
                builder
                    .path("/api/typ02/openApi/VilageFcstInfoService_2.0/$endpoint")
                    .queryParam("authKey", authKey)
                    .queryParam("numOfRows", 1000)
                    .queryParam("pageNo", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
            }.retrieve()
            .bodyToMono<WeatherApiResponse>()
            .onErrorResume { e ->
                log.error(e) { "$label API 호출 실패 - baseDate: $baseDate, baseTime: $baseTime, nx: $nx, ny: $ny" }
                Mono.empty()
            }
    }

    private fun getAuthKey(): String =
        (
            configurationRepository.findByKey(WEATHER_API_KEY)
                ?: throw CustomException(SafersErrorCode.NOT_FOUND_CONFIGURATION, WEATHER_API_KEY)
        ).value
}
