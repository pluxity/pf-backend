package com.pluxity.safers.llm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm")
data class LlmProperties(
    val ollama: OllamaProperties = OllamaProperties(),
    val gemini: GeminiProperties = GeminiProperties(),
    val temperature: Double = 0.1,
    val timeoutMs: Int = 60000,
) {
    val availableProviders: List<LlmProvider>
        get() =
            buildList {
                if (ollama.isEnabled) add(LlmProvider.OLLAMA)
                if (gemini.isEnabled) add(LlmProvider.GEMINI)
            }
}

data class OllamaProperties(
    val baseUrl: String = "",
    val model: String = "",
) {
    val isEnabled: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank()
}

data class GeminiProperties(
    val apiKey: String = "",
    val model: String = "",
    val dailyLimit: Int = 9500,
) {
    val isEnabled: Boolean
        get() = apiKey.isNotBlank() && model.isNotBlank()
}

enum class LlmProvider {
    OLLAMA,
    GEMINI,
}
