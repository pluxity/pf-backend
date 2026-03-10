package com.pluxity.weekly.chat.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ollama")
data class OllamaProperties(
    val baseUrl: String = "http://192.168.10.183:11434",
    val model: String = "report-extractor",
    val temperature: Double = 0.1,
    val timeoutMs: Int = 60000,
)
