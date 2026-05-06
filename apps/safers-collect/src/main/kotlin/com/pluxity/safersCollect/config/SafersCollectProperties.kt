package com.pluxity.safersCollect.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "safety-collector")
data class SafersCollectProperties(
    val buffer: Buffer,
) {
    data class Buffer(
        val inMemoryMax: Int,
    )
}
