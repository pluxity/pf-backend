package com.pluxity.weekly.chat.dto

import com.pluxity.weekly.epic.entity.EpicStatus

data class EpicSearchFilter(
    val status: EpicStatus? = null,
    val name: String? = null,
    val projectId: Long? = null,
    val assigneeId: Long? = null,
)
