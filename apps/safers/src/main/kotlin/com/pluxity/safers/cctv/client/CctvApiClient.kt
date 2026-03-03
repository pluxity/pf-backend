package com.pluxity.safers.cctv.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.dto.MediaServerPathListResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class CctvApiClient(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val webClientFactory: WebClientFactory,
) {
    fun fetchPaths(baseUrl: String): List<MediaServerPathItem> {
        val client = webClientFactory.createClient(baseUrl)
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
