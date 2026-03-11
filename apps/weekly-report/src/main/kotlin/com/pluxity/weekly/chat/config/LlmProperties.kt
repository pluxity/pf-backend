package com.pluxity.weekly.chat.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm")
data class LlmProperties(
    val provider: String = "openai",
    val baseUrl: String = "https://api.groq.com/openai",
    val model: String = "openai/gpt-oss-20b",
    val apiKey: String = "",
    val temperature: Double = 0.1,
    val timeoutMs: Int = 60000,
)
