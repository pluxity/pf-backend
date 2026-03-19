package com.pluxity.safers.llm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm")
data class LlmProperties(
    val ollama: OllamaProperties = OllamaProperties(),
    val openrouter: OpenRouterProperties = OpenRouterProperties(),
    val temperature: Double = 0.1,
    val timeoutMs: Int = 60000,
) {
    val availableProviders: List<LlmProvider>
        get() =
            buildList {
                if (ollama.isEnabled) add(LlmProvider.OLLAMA)
                if (openrouter.isEnabled) add(LlmProvider.OPENROUTER)
            }
}

data class OllamaProperties(
    val baseUrl: String = "",
    val model: String = "",
) {
    val isEnabled: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank()
}

data class OpenRouterProperties(
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
) {
    val isEnabled: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
}

enum class LlmProvider {
    OLLAMA,
    OPENROUTER,
}
