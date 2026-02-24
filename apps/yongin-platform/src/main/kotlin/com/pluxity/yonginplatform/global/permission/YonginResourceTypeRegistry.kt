package com.pluxity.yonginplatform.global.permission

import com.pluxity.common.auth.permission.ResourceTypeInfo
import com.pluxity.common.auth.permission.ResourceTypeRegistry
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import org.springframework.stereotype.Component

@Component
class YonginResourceTypeRegistry : ResourceTypeRegistry {
    enum class YonginResourceType(
        val resourceName: String,
        val endpoint: String,
    ) {
        USER("사용자관리", "users"),
        ATTENDANCE_STATUS("출역현황", "attendances"),
        PROCESS_STATUS("공정현황", "process-statuses"),
        KEY_MANAGEMENT("주요관리사항", "key-management"),
        GOAL("목표관리", "goals"),
    }

    override fun resolve(name: String): String =
        YonginResourceType.entries
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.name
            ?: throw CustomException(ErrorCode.INVALID_RESOURCE_TYPE, name)

    override fun allEntries(): List<ResourceTypeInfo> =
        YonginResourceType.entries.map {
            ResourceTypeInfo(
                key = it.name,
                resourceName = it.resourceName,
                endpoint = it.endpoint,
            )
        }
}
