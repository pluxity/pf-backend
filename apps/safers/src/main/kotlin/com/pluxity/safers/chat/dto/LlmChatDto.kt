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
    val filters: ActionFilter,
    val page: Int = 1,
    val size: Int = 50,
)

enum class QueryTarget {
    EVENT,
    CCTV,
    WEATHER,
    SITE,
}
