package com.pluxity.weekly.chat.dto

import com.pluxity.weekly.project.entity.ProjectStatus

data class ProjectSearchFilter(
    val status: ProjectStatus? = null,
    val name: String? = null,
)
