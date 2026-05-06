package com.pluxity.safersCollect.forwarder

import com.pluxity.safersCollect.buffer.TelemetryBuffer
import com.pluxity.safersCollect.config.SafersCollectProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class TelemetryForwarder(
    private val buffer: TelemetryBuffer,
    private val client: CentralIngestClient,
    private val props: SafersCollectProperties,
) {
    @Scheduled(fixedDelayString = "\${safety-collector.forwarder.flush-interval-ms}")
    fun flush() {
        val batch = buffer.drain(props.forwarder.batchSize)
        if (batch.isEmpty()) return

        try {
            client.postTelemetry(batch)
        } catch (e: Exception) {
            log.warn(e) { "telemetry forward 실패 — ${batch.size} envelope 재enqueue" }
            buffer.enqueueAll(batch)
        }
    }
}
