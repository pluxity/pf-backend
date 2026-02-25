package com.pluxity.cctv.client

import com.pluxity.cctv.config.CctvErrorCode
import com.pluxity.cctv.config.MediaServerProperties
import com.pluxity.cctv.dto.MediaServerPathItem
import com.pluxity.cctv.dto.MediaServerPathListResponse
import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.common.core.exception.CustomException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class CctvApiClient(
    webClientFactory: WebClientFactory,
    private val mediaServerProperties: MediaServerProperties,
) {
    private val client: WebClient = webClientFactory.createClient(mediaServerProperties.url)

    fun fetchPaths(): List<MediaServerPathItem> {
        if (mediaServerProperties.url.isBlank()) {
            throw CustomException(CctvErrorCode.MEDIA_SERVER_URL_NOT_CONFIGURED)
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
