package com.pluxity.common.auth.user.repository

import com.pluxity.common.auth.permission.Permission
import com.pluxity.common.auth.user.entity.Role
import com.pluxity.common.auth.user.entity.RolePermission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RolePermissionRepository : JpaRepository<RolePermission, Long> {
    fun deleteAllByPermission(permission: Permission)

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :role")
    fun deleteAllByRole(
        @Param("role") role: Role,
    )
}
