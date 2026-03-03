package com.pluxity.safers.cctv.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.safers.cctv.config.MediaServerProperties
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.dto.MediaServerPathListResponse
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
