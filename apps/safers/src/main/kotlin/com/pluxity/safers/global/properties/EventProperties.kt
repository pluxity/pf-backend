package com.pluxity.safers.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "event")
data class EventProperties(
    val baseUrl: String,
)
