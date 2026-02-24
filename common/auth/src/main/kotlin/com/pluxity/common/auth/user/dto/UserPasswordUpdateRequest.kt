package com.pluxity.common.auth.user.dto

data class UserPasswordUpdateRequest(
    val currentPassword: String,
    val newPassword: String,
)
