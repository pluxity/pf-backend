package com.pluxity.safersCollect.config

import com.pluxity.safersCollect.queue.DropPolicy
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "safety-collector")
data class SafersCollectProperties(
    val site: Site,
    val central: Central,
    val queue: Queue,
    val forwarder: Forwarder,
    val mqtt: Mqtt,
) {
    data class Site(
        val id: Long,
    )

    data class Central(
        val ingestUrl: String,
        val apiKey: String,
    )

    data class Queue(
        val normalCapacity: Int,
        val dropPolicy: DropPolicy = DropPolicy.DROP_OLDEST,
    )

    data class Forwarder(
        val batchSize: Int,
        val flushIntervalMs: Long,
    )

    data class Mqtt(
        val host: String,
        val port: Int,
        val clientId: String,
        val topicPrefix: String,
    )
}
