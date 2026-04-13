package com.pluxity.common.auth.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val additionalOriginPatterns: List<String> = emptyList(),
)
