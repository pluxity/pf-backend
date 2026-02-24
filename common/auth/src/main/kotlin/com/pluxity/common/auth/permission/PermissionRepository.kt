package com.pluxity.common.auth.permission

import org.springframework.data.jpa.repository.JpaRepository

interface PermissionRepository : JpaRepository<Permission, Long> {
    fun existsByName(permissionName: String): Boolean

    fun existsByNameAndIdNot(
        newName: String,
        id: Long,
    ): Boolean
}
