package com.pluxity.weekly.chat.dto

sealed interface ChatDto {
    val name: String?
}

data class ProjectChatDto(
    override val name: String?,
    val description: String?,
    val status: String?,
    val startDate: String?,
    val dueDate: String?,
    val pmId: Long?,
) : ChatDto

data class EpicChatDto(
    override val name: String?,
    val projectId: Long?,
    val description: String?,
    val status: String?,
    val startDate: String?,
    val dueDate: String?,
    val userIds: List<Long>?,
) : ChatDto

data class TaskChatDto(
    override val name: String?,
    val epicId: Long?,
    val description: String?,
    val status: String?,
    val progress: Int?,
    val startDate: String?,
    val dueDate: String?,
    val assigneeId: Long?,
) : ChatDto

data class TeamChatDto(
    override val name: String?,
    val leaderId: Long?,
) : ChatDto
