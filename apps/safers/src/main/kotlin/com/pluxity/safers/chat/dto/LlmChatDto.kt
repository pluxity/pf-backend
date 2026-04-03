package com.pluxity.safers.chat.dto

/**
 * 1차 LLM 응답: 의도 파악 (actions + summary)
 */
data class IntentResult(
    val summary: String,
    val actions: List<QueryAction>,
)

data class QueryAction(
    val id: String,
    val target: QueryTarget,
    val filters: Map<String, Any?>,
)

enum class QueryTarget {
    EVENT,
    CCTV,
    WEATHER,
    SITE,
}

/**
 * 2차 LLM 응답: UI 배치 (surfaceUpdate)
 */
data class SurfaceUpdate(
    val surfaceId: String = "main",
    val components: List<Map<String, Any>>,
)
