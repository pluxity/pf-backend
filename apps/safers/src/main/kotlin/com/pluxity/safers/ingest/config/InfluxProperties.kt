package com.pluxity.safers.ingest.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "influx")
data class InfluxProperties(
    val url: String,
    val token: String,
    val org: String,
    val bucket: String,
    val batchSize: Int = 1000, // 1000건 모이면 flush
    val flushIntervalMs: Int = 1000, // 또는 1초마다 강제 flush (먼저 도달)
    val bufferLimit: Int = 10000, // buffer 1만 건 초과 시 백프레셔
    val retryIntervalMs: Int = 5000, // 실패 시 5초 후 재시도
    val maxRetries: Int = 3, // 최대 3번 재시도
)
