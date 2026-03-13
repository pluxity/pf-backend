package com.pluxity.weekly.chat.action.dto

data class ActionResult(
    val type: ActionResultType,
    val action: ActionType,
    val message: String,
    val data: Any? = null,
    val candidates: List<Map<String, String>>? = null,
    val partial: Map<String, Any?>? = null,
    val target: String? = null,
    val requiredFields: List<FieldSpec>? = null,
)

data class FieldSpec(
    val key: String,
    val label: String,
    val type: String = "text",
    val required: Boolean = false,
    val options: List<Map<String, String>>? = null,
)

enum class ActionResultType {
    SUCCESS,
    NEEDS_CONFIRM,
    CLARIFY,
    ERROR,
}

enum class ActionType {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    CLARIFY,
    UNKNOWN,
}
