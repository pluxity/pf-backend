package com.pluxity.common.test.util

import com.pluxity.common.auth.permission.DomainPermission
import com.pluxity.common.auth.permission.Permission
import com.pluxity.common.auth.permission.PermissionLevel
import com.pluxity.common.auth.permission.ResourcePermission
import com.pluxity.common.auth.user.entity.Role
import com.pluxity.common.auth.user.entity.RolePermission
import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.auth.user.service.UserService
import com.pluxity.common.core.test.withId
import io.mockk.every
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

fun makeUserWithPermissions(
    resourcePermissions: List<ResourcePermission>,
    domainPermissions: List<DomainPermission> = emptyList(),
): User {
    val role = Role(name = "ROLE_USER", description = "role").withId(1L)
    if (resourcePermissions.isNotEmpty() || domainPermissions.isNotEmpty()) {
        val permission = Permission("권한 그룹", null)
        resourcePermissions.forEach { permission.addResourcePermission(it) }
        domainPermissions.forEach { permission.addDomainPermission(it) }
        role.addRolePermission(RolePermission(role = role, permission = permission))
    }
    return User("tester", "pw", "name", null).withId(10L).apply { addRole(role) }
}

fun setUserWithPermission(
    userService: UserService,
    resourceType: String,
    level: PermissionLevel,
    resourceId: String,
) {
    val user =
        makeUserWithPermissions(
            resourcePermissions =
                listOf(
                    ResourcePermission(
                        resourceName = resourceType,
                        resourceId = resourceId,
                        level = level,
                    ),
                ),
            domainPermissions = emptyList(),
        )
    every { userService.findUserByUsername("tester") } returns user
}

fun setUserWithPermission(
    userService: UserService,
    resourceType: String,
    level: PermissionLevel,
    resourceId: Long,
) {
    setUserWithPermission(userService, resourceType, level, resourceId.toString())
}

fun setUserWithPermissions(
    userService: UserService,
    resourcePermissions: List<ResourcePermission>,
    domainPermissions: List<DomainPermission> = emptyList(),
) {
    val user = makeUserWithPermissions(resourcePermissions, domainPermissions)
    every { userService.findUserByUsername("tester") } returns user
}

fun initAuthUser(userService: UserService) {
    val user = makeUserWithPermissions(emptyList(), emptyList())
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken("tester", null, emptyList())
    every { userService.findUserByUsername("tester") } returns user
}
