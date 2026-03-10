package com.pluxity.weekly.chat.action.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class LlmAction(
    val action: String,
    val id: Long? = null,
    val name: String? = null,
    val description: String? = null,
    val status: String? = null,
    val progress: Int? = null,
    val project: String? = null,
    @JsonProperty("project_id")
    val projectId: Long? = null,
    val epic: String? = null,
    @JsonProperty("epic_id")
    val epicId: Long? = null,
    @JsonProperty("start_date")
    val startDate: String? = null,
    @JsonProperty("due_date")
    val dueDate: String? = null,
    val filters: Map<String, Any?>? = null,
    val message: String? = null,
    val candidates: List<String>? = null,
    val partial: Map<String, Any?>? = null,
)
