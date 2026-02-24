package com.pluxity.common.test.dto

import com.pluxity.common.auth.user.dto.UserCreateRequest
import com.pluxity.common.auth.user.dto.UserPasswordUpdateRequest
import com.pluxity.common.auth.user.dto.UserRoleUpdateRequest
import com.pluxity.common.auth.user.dto.UserUpdateRequest

fun dummyUserCreateRequest(
    username: String = "username",
    password: String = "password",
    name: String = "name",
    code: String = "code",
    phoneNumber: String? = null,
    department: String? = null,
    roleIds: List<Long> = listOf(),
): UserCreateRequest =
    UserCreateRequest(
        username,
        password,
        name,
        code,
        phoneNumber,
        department,
        roleIds,
    )

fun dummyUserUpdateRequest(
    name: String = "name",
    code: String = "code",
    phoneNumber: String? = null,
    department: String? = null,
    roleIds: List<Long>? = null,
): UserUpdateRequest = UserUpdateRequest(name, code, phoneNumber, department, roleIds)

fun dummyUserRoleAssignRequest(roleIds: List<Long> = listOf()): UserRoleUpdateRequest = UserRoleUpdateRequest(roleIds)

fun dummyUserPasswordUpdateRequest(
    currentPassword: String = "currentPassword",
    newPassword: String = "newPassword",
): UserPasswordUpdateRequest = UserPasswordUpdateRequest(currentPassword, newPassword)
