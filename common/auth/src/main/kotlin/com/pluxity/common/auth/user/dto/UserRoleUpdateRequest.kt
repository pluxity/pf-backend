package com.pluxity.common.auth.user.dto

data class UserRoleUpdateRequest(
    val roleIds: List<Long> = emptyList(),
)
