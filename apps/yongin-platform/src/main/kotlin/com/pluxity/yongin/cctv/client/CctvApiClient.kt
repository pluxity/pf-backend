package com.pluxity.yongin.cctv.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.yongin.cctv.config.MediaServerProperties
import com.pluxity.yongin.cctv.dto.MediaServerPathItem
import com.pluxity.yongin.cctv.dto.MediaServerPathListResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class CctvApiClient(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    webClientFactory: WebClientFactory,
    mediaServerProperties: MediaServerProperties,
) {
    private val client: WebClient = webClientFactory.createClient(mediaServerProperties.url)

    fun fetchPaths(): List<MediaServerPathItem> {
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
