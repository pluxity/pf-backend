package com.pluxity.safers.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.kafka")
data class KafkaProperties(
    val bootstrapServers: String,
)
