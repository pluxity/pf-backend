package com.pluxity.common.auth.test

import com.pluxity.common.auth.authentication.entity.RefreshToken
import com.pluxity.common.auth.permission.Permission
import com.pluxity.common.auth.user.dto.UserCreateRequest
import com.pluxity.common.auth.user.dto.UserPasswordUpdateRequest
import com.pluxity.common.auth.user.dto.UserRoleUpdateRequest
import com.pluxity.common.auth.user.dto.UserUpdateRequest
import com.pluxity.common.auth.user.entity.Role
import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId

fun dummyUser(
    id: Long? = 1L,
    username: String = "username",
    password: String = "password",
    name: String = "name",
    code: String? = "code",
    phoneNumber: String? = null,
    department: String? = null,
): User = User(username, password, name, code, phoneNumber, department).withId(id)

fun dummyRole(
    id: Long? = 1L,
    name: String = "name",
    description: String? = "description",
): Role = Role(name, description).withId(id)

fun dummyPermission(
    id: Long? = 1L,
    name: String = "name",
    description: String? = "description",
): Permission = Permission(name, description).withAudit().withId(id)

fun dummyRefreshToken(
    username: String = "username",
    token: String = "token",
    timeToLive: Int = 30,
): RefreshToken = RefreshToken(username, token, timeToLive)

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
