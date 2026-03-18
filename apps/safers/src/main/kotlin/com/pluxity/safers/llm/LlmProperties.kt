package com.pluxity.safers.llm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm")
data class LlmProperties(
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val temperature: Double = 0.1,
    val timeoutMs: Int = 60000,
)
