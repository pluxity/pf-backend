package com.pluxity.safers.weather.client

import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.configuration.service.ConfigurationService
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.weather.dto.WeatherApiResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.PrematureCloseException
import reactor.netty.resources.ConnectionProvider
import reactor.util.retry.Retry
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.time.Duration
import io.netty.handler.timeout.TimeoutException as NettyTimeoutException

private val log = KotlinLogging.logger {}

@Component
class WeatherApiClient(
    private val configurationService: ConfigurationService,
) {
    private val webClient = buildWebClient()

    companion object {
        private const val BASE_URL = "https://apihub.kma.go.kr"
        private const val WEATHER_API_KEY = "WEATHER_API"
        private const val MAX_IN_MEMORY_SIZE = 1 * 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val RESPONSE_TIMEOUT_SEC = 20L
        private const val IO_TIMEOUT_SEC = 20
        private const val MAX_RETRY_ATTEMPTS = 2L
        private const val SUCCESS_RESULT_CODE = "00"
        private val OVERALL_TIMEOUT = Duration.ofSeconds(90)
        private val POOL_MAX_IDLE = Duration.ofSeconds(15)
        private val POOL_MAX_LIFE = Duration.ofSeconds(120)
        private val POOL_PENDING_ACQUIRE = Duration.ofSeconds(30)
        private val POOL_EVICT_INTERVAL = Duration.ofSeconds(30)
        private const val POOL_MAX_CONNECTIONS = 50
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
            .flatMap { response ->
                val header = response.response.header
                if (header.resultCode == SUCCESS_RESULT_CODE) {
                    Mono.just(response)
                } else {
                    log.warn {
                        "$label API 비정상 응답 - resultCode: ${header.resultCode}, resultMsg: ${header.resultMsg}, " +
                            "baseDate: $baseDate, baseTime: $baseTime, nx: $nx, ny: $ny"
                    }
                    Mono.empty()
                }
            }.retryWhen(
                Retry
                    .backoff(MAX_RETRY_ATTEMPTS, Duration.ofMillis(500))
                    .maxBackoff(Duration.ofSeconds(3))
                    .filter(::isRetryable)
                    .doBeforeRetry { signal ->
                        log.warn {
                            "$label API 재시도 #${signal.totalRetries() + 1} - baseDate: $baseDate, baseTime: $baseTime, " +
                                "nx: $nx, ny: $ny, cause: ${signal.failure().rootMessage()}"
                        }
                    },
            ).timeout(OVERALL_TIMEOUT)
            .onErrorResume { e ->
                log.error(e) { "$label API 호출 실패 - baseDate: $baseDate, baseTime: $baseTime, nx: $nx, ny: $ny" }
                Mono.empty()
            }
    }

    private fun isRetryable(throwable: Throwable): Boolean {
        var cur: Throwable? = throwable
        while (cur != null) {
            when (cur) {
                is PrematureCloseException -> return true
                is ConnectException -> return true
                is SocketException ->
                    if (cur.message?.contains("reset", ignoreCase = true) == true) return true
                is NettyTimeoutException -> return true
                is SocketTimeoutException -> return true
            }
            cur = cur.cause
        }
        return false
    }

    private fun Throwable.rootMessage(): String {
        var cur: Throwable = this
        while (cur.cause != null && cur.cause !== cur) cur = cur.cause!!
        return "${cur::class.simpleName}: ${cur.message ?: ""}"
    }

    private fun buildWebClient(): WebClient {
        val connectionProvider =
            ConnectionProvider
                .builder("kma-weather-pool")
                .maxConnections(POOL_MAX_CONNECTIONS)
                .maxIdleTime(POOL_MAX_IDLE)
                .maxLifeTime(POOL_MAX_LIFE)
                .pendingAcquireTimeout(POOL_PENDING_ACQUIRE)
                .evictInBackground(POOL_EVICT_INTERVAL)
                .build()

        val httpClient =
            HttpClient
                .create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SEC))
                .doOnConnected { conn ->
                    conn
                        .addHandlerLast(ReadTimeoutHandler(IO_TIMEOUT_SEC))
                        .addHandlerLast(WriteTimeoutHandler(IO_TIMEOUT_SEC))
                }

        return WebClient
            .builder()
            .baseUrl(BASE_URL)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE) }
            .build()
    }

    private fun getAuthKey(): String =
        configurationService.findValue(WEATHER_API_KEY)
            ?: throw CustomException(SafersErrorCode.NOT_FOUND_CONFIGURATION, WEATHER_API_KEY)
}
