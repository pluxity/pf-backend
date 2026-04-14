package com.pluxity.safers.cctv.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.dto.MediaServerPathListResponse
import com.pluxity.safers.cctv.dto.MediaServerPlaybackResponse
import com.pluxity.safers.global.constant.SafersErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Component
class CctvApiClient(
    private val webClientFactory: WebClientFactory,
) {
    companion object {
        private val UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    private val sitePortMap =
        mapOf(
            9L to 9904,
            11L to 9901,
            13L to 9902,
            17L to 9903,
            16L to 9905,
            10L to 9906,
            12L to 9907,
            15L to 9908,
            14L to 9909,
            18L to 9911,
        )

    fun requestPlayback(
        baseUrl: String,
        siteId: Long,
        nvrId: String,
        channel: Int,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): MediaServerPlaybackResponse {
        val client = createClientForSite(baseUrl, siteId)
        return client
            .post()
            .uri("/v3/nvr/$nvrId/playback")
            .bodyValue(
                mapOf(
                    "channel" to channel,
                    "startTime" to UTC_FORMATTER.format(startTime),
                    "endTime" to UTC_FORMATTER.format(endTime),
                ),
            ).retrieve()
            .bodyToMono<MediaServerPlaybackResponse>()
            .block()
            ?: throw CustomException(SafersErrorCode.PLAYBACK_REQUEST_FAILED)
    }

    fun deletePlayback(
        baseUrl: String,
        siteId: Long,
        nvrId: String,
        pathName: String,
    ) {
        try {
            val client = createClientForSite(baseUrl, siteId)
            client
                .delete()
                .uri("/v3/nvr/$nvrId/playback/$pathName")
                .retrieve()
                .bodyToMono<Void>()
                .block()
        } catch (e: Exception) {
            log.warn(e) { "Playback 세션 삭제 실패 - nvrId: $nvrId, pathName: $pathName" }
        }
    }

    fun fetchPaths(
        baseUrl: String,
        siteId: Long,
    ): List<MediaServerPathItem> {
        val client = createClientForSite(baseUrl, siteId)
        val response =
            client
                .get()
                .uri("/v3/config/paths/list")
                .retrieve()
                .bodyToMono<MediaServerPathListResponse>()
                .block()
        return response?.items ?: emptyList()
    }

    private fun createClientForSite(
        baseUrl: String,
        siteId: Long,
    ): WebClient {
        val port = sitePortMap[siteId]
        val url = if (port != null) "$baseUrl:$port" else baseUrl
        return webClientFactory.createClient(url)
    }
}
