package com.pluxity.weekly.chat.action.dto

data class ResolveAction(
    override val action: String,
    override val target: String? = null,
    override val id: Long? = null,
    override val name: String? = null,
    override val description: String? = null,
    override val status: String? = null,
    override val progress: Int? = null,
    override val project: String? = null,
    override val projectId: Long? = null,
    override val epic: String? = null,
    override val epicId: Long? = null,
    override val startDate: String? = null,
    override val dueDate: String? = null,
    override val filters: Map<String, Any?>? = null,
    val pmId: Long? = null,
    val step: Int? = null,
) : ActionRequest
