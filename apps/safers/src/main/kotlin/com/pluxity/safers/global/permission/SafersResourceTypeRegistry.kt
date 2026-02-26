package com.pluxity.safers.global.permission

import com.pluxity.common.auth.permission.ResourceTypeInfo
import com.pluxity.common.auth.permission.ResourceTypeRegistry
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import org.springframework.stereotype.Component

@Component
class SafersResourceTypeRegistry : ResourceTypeRegistry {
    enum class SafersResourceType(
        val resourceName: String,
        val endpoint: String,
    ) {
        USER("사용자관리", "users"),
        SITE("현장관리", "sites"),
    }

    override fun resolve(name: String): String =
        SafersResourceType.entries
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.name
            ?: throw CustomException(ErrorCode.INVALID_RESOURCE_TYPE, name)

    override fun allEntries(): List<ResourceTypeInfo> =
        SafersResourceType.entries.map {
            ResourceTypeInfo(
                key = it.name,
                resourceName = it.resourceName,
                endpoint = it.endpoint,
            )
        }
}
