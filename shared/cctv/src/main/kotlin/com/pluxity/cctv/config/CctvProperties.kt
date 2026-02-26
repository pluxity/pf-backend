package com.pluxity.cctv.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cctv")
data class CctvProperties(
    val maxBookmarkCount: Int = 4,
)
