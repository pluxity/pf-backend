package com.pluxity.safers.chat.dto

/**
 * 1차 LLM 응답: 의도 파악 (mode + actions/ref/patch)
 */
data class IntentResult(
    val summary: String,
    val mode: IntentMode = IntentMode.NEW,
    val actions: List<QueryAction> = emptyList(),
    val ref: String? = null,
    val patch: PatchAction? = null,
)

enum class IntentMode {
    NEW,
    RECALL,
    MODIFY,
}

data class PatchAction(
    val add: List<QueryAction> = emptyList(),
    val remove: List<String> = emptyList(),
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
