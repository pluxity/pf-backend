package com.pluxity.safersCollect.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "safety-collector")
data class SafersCollectProperties(
    val site: Site,
    val central: Central,
    val buffer: Buffer,
    val forwarder: Forwarder,
) {
    data class Site(
        val id: Long,
    )

    data class Central(
        val ingestUrl: String,
        val apiKey: String,
    )

    data class Buffer(
        val inMemoryMax: Int,
    )

    data class Forwarder(
        val batchSize: Int,
        val flushIntervalMs: Long,
    )
}
