package com.pluxity.weekly.chat.action.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class LlmAction(
    override val action: String,
    override val target: String? = null,
    override val id: Long? = null,
    override val name: String? = null,
    override val description: String? = null,
    override val status: String? = null,
    override val progress: Int? = null,
    override val project: String? = null,
    @param:JsonProperty("project_id")
    override val projectId: Long? = null,
    override val epic: String? = null,
    @param:JsonProperty("epic_id")
    override val epicId: Long? = null,
    @param:JsonProperty("start_date")
    override val startDate: String? = null,
    @param:JsonProperty("due_date")
    override val dueDate: String? = null,
    override val filters: Map<String, Any?>? = null,
    val message: String? = null,
    val candidates: List<Map<String, String>>? = null,
    val partial: Map<String, Any?>? = null,
) : ActionRequest
