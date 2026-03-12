package com.pluxity.weekly.chat.action.dto

sealed interface ActionRequest {
    val action: String
    val target: String?
    val id: Long?
    val name: String?
    val description: String?
    val status: String?
    val progress: Int?
    val project: String?
    val projectId: Long?
    val epic: String?
    val epicId: Long?
    val startDate: String?
    val dueDate: String?
    val filters: Map<String, Any?>?
}
