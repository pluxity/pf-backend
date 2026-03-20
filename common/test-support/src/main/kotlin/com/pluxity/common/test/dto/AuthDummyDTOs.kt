package com.pluxity.common.test.dto

import com.pluxity.common.auth.authentication.dto.SignInRequest
import com.pluxity.common.auth.authentication.dto.SignUpRequest
import com.pluxity.common.auth.permission.PermissionLevel
import com.pluxity.common.auth.permission.dto.PermissionCreateRequest
import com.pluxity.common.auth.permission.dto.PermissionRequest
import com.pluxity.common.auth.permission.dto.PermissionUpdateRequest
import com.pluxity.common.auth.user.dto.RoleCreateRequest
import com.pluxity.common.auth.user.dto.RoleUpdateRequest
import com.pluxity.common.auth.user.dto.UserCreateRequest
import com.pluxity.common.auth.user.dto.UserPasswordUpdateRequest
import com.pluxity.common.auth.user.dto.UserRoleUpdateRequest
import com.pluxity.common.auth.user.dto.UserUpdateRequest
import com.pluxity.common.auth.user.entity.RoleType

fun dummyUserCreateRequest(
    username: String = "username",
    password: String = "password",
    name: String = "name",
    code: String = "code",
    phoneNumber: String? = null,
    department: String? = null,
    profileImageId: Long? = null,
    roleIds: List<Long> = listOf(),
): UserCreateRequest =
    UserCreateRequest(
        username = username,
        password = password,
        name = name,
        code = code,
        phoneNumber = phoneNumber,
        department = department,
        profileImageId = profileImageId,
        roleIds = roleIds,
    )

fun dummyUserUpdateRequest(
    name: String = "name",
    code: String = "code",
    phoneNumber: String? = null,
    department: String? = null,
    profileImageId: Long? = null,
    roleIds: List<Long>? = null,
): UserUpdateRequest =
    UserUpdateRequest(
        name = name,
        code = code,
        phoneNumber = phoneNumber,
        department = department,
        profileImageId = profileImageId,
        roleIds = roleIds,
    )

fun dummyUserRoleAssignRequest(roleIds: List<Long> = listOf()): UserRoleUpdateRequest = UserRoleUpdateRequest(roleIds)

fun dummyUserPasswordUpdateRequest(
    currentPassword: String = "currentPassword",
    newPassword: String = "newPassword",
): UserPasswordUpdateRequest = UserPasswordUpdateRequest(currentPassword, newPassword)

fun dummySignUpRequest(
    username: String = "username",
    password: String = "password",
    name: String = "name",
    code: String = "code",
): SignUpRequest = SignUpRequest(username, password, name, code)

fun dummySignInRequest(
    username: String = "username",
    password: String = "password",
): SignInRequest = SignInRequest(username, password)

fun dummyRoleCreateRequest(
    name: String = "일반 사용자",
    description: String? = "기본 역할",
    permissionIds: List<Long> = emptyList(),
    authority: RoleType = RoleType.USER,
): RoleCreateRequest = RoleCreateRequest(name, description, permissionIds, authority)

fun dummyRoleUpdateRequest(
    name: String? = "수정된 역할",
    description: String? = "수정된 설명",
    permissionIds: List<Long>? = emptyList(),
): RoleUpdateRequest = RoleUpdateRequest(name, description, permissionIds)

fun dummyPermissionCreateRequest(
    name: String = "건물 읽기 권한",
    description: String? = "건물 조회만 가능",
    permissions: List<PermissionRequest> = listOf(dummyPermissionRequest()),
): PermissionCreateRequest = PermissionCreateRequest(name, description, permissions)

fun dummyPermissionUpdateRequest(
    name: String? = "수정된 권한",
    description: String? = "수정된 설명",
    permissions: List<PermissionRequest> = listOf(dummyPermissionRequest()),
): PermissionUpdateRequest = PermissionUpdateRequest(name, description, permissions)

fun dummyPermissionRequest(
    resourceType: String = "BUILDING",
    resourceIds: List<String> = emptyList(),
    level: PermissionLevel = PermissionLevel.READ,
): PermissionRequest = PermissionRequest(resourceType, resourceIds, level)
