package com.pluxity.common.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pluxity.logbook")
data class LogbookProperties(
    val excludePaths: List<String> = emptyList(),
)
