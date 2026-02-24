package com.pluxity.common.auth.user.service

import com.pluxity.common.auth.user.entity.UserResourcePermission
import com.pluxity.common.auth.user.repository.UserResourcePermissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserResourcePermissionService(
    private val userResourcePermissionRepository: UserResourcePermissionRepository,
) {
    fun exists(
        userId: Long,
        resourceType: String,
        resourceId: String,
    ): Boolean =
        userResourcePermissionRepository.existsByUserIdAndResourceTypeAndResourceId(
            userId,
            resourceType,
            resourceId,
        )

    @Transactional
    fun create(
        userId: Long,
        resourceType: String,
        resourceId: String,
    ) {
        if (exists(userId, resourceType, resourceId)) {
            return
        }

        userResourcePermissionRepository.save(
            UserResourcePermission(
                userId = userId,
                resourceType = resourceType,
                resourceId = resourceId,
            ),
        )
    }

    @Transactional
    fun delete(
        resourceType: String,
        resourceId: String,
    ) {
        userResourcePermissionRepository.deleteByResourceTypeAndResourceId(
            resourceType,
            resourceId,
        )
    }
}
