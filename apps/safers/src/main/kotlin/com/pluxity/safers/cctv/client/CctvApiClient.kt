package com.pluxity.safers.cctv.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.dto.MediaServerPathListResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class CctvApiClient(
    private val webClientFactory: WebClientFactory,
) {
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
        )

    fun fetchPaths(
        baseUrl: String,
        siteId: Long,
    ): List<MediaServerPathItem> {
        val port = sitePortMap[siteId]
        val url = if (port != null) "$baseUrl:$port" else baseUrl
        val client = webClientFactory.createClient(url)
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
