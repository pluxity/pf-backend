package com.pluxity.common.auth.user.dto

import com.pluxity.common.auth.permission.dto.PermissionResponse
import com.pluxity.common.auth.permission.dto.toResponse
import com.pluxity.common.auth.user.entity.Role

data class RoleResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val permissions: List<PermissionResponse>,
)

fun Role.toResponse() =
    RoleResponse(
        this.requiredId,
        this.name,
        this.description,
        this.rolePermissions
            .map { it.permission }
            .sortedByDescending { it.id }
            .map { it.toResponse() }
            .toList(),
    )
