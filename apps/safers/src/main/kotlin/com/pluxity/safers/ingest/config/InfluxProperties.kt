package com.pluxity.safers.ingest.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "influx")
data class InfluxProperties(
    val url: String,
    val token: String,
    val org: String,
    val bucket: String,
)
