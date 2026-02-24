package com.pluxity.common.auth.user.repository

import com.pluxity.common.auth.user.entity.UserResourcePermission
import org.springframework.data.jpa.repository.JpaRepository

interface UserResourcePermissionRepository : JpaRepository<UserResourcePermission, Long> {
    fun existsByUserIdAndResourceTypeAndResourceId(
        userId: Long,
        resourceType: String,
        resourceId: String,
    ): Boolean

    fun deleteByResourceTypeAndResourceId(
        resourceType: String,
        resourceId: String,
    )
}
