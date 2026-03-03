package com.pluxity.safers.cctv.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "media-server")
data class MediaServerProperties(
    val url: String,
)
