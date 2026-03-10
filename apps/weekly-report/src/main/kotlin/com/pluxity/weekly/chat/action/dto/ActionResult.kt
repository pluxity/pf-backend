package com.pluxity.weekly.chat.action.dto

data class ActionResult(
    val type: ActionResultType,
    val message: String,
    val data: Any? = null,
    val candidates: List<String>? = null,
    val partial: Map<String, Any?>? = null,
)

enum class ActionResultType {
    SUCCESS,
    NEEDS_CONFIRM,
    CLARIFY,
    ERROR,
}
