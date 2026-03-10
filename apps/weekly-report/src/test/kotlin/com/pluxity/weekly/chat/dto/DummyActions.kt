package com.pluxity.weekly.chat.dto

import com.pluxity.weekly.chat.action.dto.LlmAction

fun dummyReadAction(
    filters: Map<String, Any?>? = null,
    project: String? = null,
    epic: String? = null,
) = LlmAction(
    action = "read",
    filters = filters,
    project = project,
    epic = epic,
)

fun dummyUpsertAction(
    id: Long? = null,
    name: String = "테스트",
    description: String? = null,
    status: String? = null,
    progress: Int? = null,
    project: String? = null,
    projectId: Long? = null,
    epic: String? = null,
    epicId: Long? = null,
    startDate: String? = null,
    dueDate: String? = null,
) = LlmAction(
    action = "upsert",
    id = id,
    name = name,
    description = description,
    status = status,
    progress = progress,
    project = project,
    projectId = projectId,
    epic = epic,
    epicId = epicId,
    startDate = startDate,
    dueDate = dueDate,
)

fun dummyDeleteAction(id: Long = 1L) =
    LlmAction(
        action = "delete",
        id = id,
    )

fun dummyClarifyAction(
    message: String = "어느 프로젝트인가요?",
    candidates: List<Map<String, String>> = listOf(
        mapOf("project" to "SAFERS", "epic" to "api 구현"),
        mapOf("project" to "용인 플랫폼", "epic" to "api 구현"),
    ),
    partial: Map<String, Any?> = mapOf("action" to "delete"),
) = LlmAction(
    action = "clarify",
    message = message,
    candidates = candidates,
    partial = partial,
)
