package com.pluxity.yongin.cctv.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.dto.MediaServerPathItem
import com.pluxity.yongin.cctv.dto.MediaServerPathListResponse
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.global.properties.MediaServerProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class CctvApiClient(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    webClientFactory: WebClientFactory,
    private val mediaServerProperties: MediaServerProperties,
) {
    private val client: WebClient = webClientFactory.createClient(mediaServerProperties.url)

    fun fetchPaths(): List<MediaServerPathItem> {
        if (mediaServerProperties.url.isBlank()) {
            throw CustomException(YonginErrorCode.MEDIA_SERVER_URL_NOT_CONFIGURED)
        }

        val response =
            client
                .get()
                .uri("/v3/paths/list")
                .retrieve()
                .bodyToMono<MediaServerPathListResponse>()
                .block()
        return response?.items ?: emptyList()
    }
}
