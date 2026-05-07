package com.pluxity.safersCollect.forwarder

import com.pluxity.safersCollect.config.SafersCollectProperties
import com.pluxity.safersCollect.forwarder.dto.EventEnvelope
import com.pluxity.safersCollect.forwarder.dto.TelemetryBatchRequest
import com.pluxity.safersCollect.forwarder.dto.TelemetryEnvelope
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class CentralIngestClient(
    private val props: SafersCollectProperties,
) {
    private val webClient: WebClient =
        WebClient
            .builder()
            .baseUrl(props.central.ingestUrl)
            .defaultHeader(API_KEY_HEADER, props.central.apiKey)
            .build()

    fun postTelemetry(envelopes: List<TelemetryEnvelope>) {
        webClient
            .post()
            .uri("/v1/sites/{siteId}/telemetry", props.site.id)
            .bodyValue(TelemetryBatchRequest(envelopes))
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    fun postEvent(envelope: EventEnvelope) {
        webClient
            .post()
            .uri("/v1/sites/{siteId}/events", props.site.id)
            .bodyValue(envelope)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    companion object {
        private const val API_KEY_HEADER = "X-Api-Key"
    }
}
