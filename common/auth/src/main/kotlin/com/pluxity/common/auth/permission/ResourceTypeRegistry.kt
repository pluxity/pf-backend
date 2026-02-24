package com.pluxity.common.auth.permission

import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException

/**
 * 앱별 ResourceType을 등록하기 위한 인터페이스.
 * 각 앱에서 자체 ResourceType enum을 정의하고 이 인터페이스를 구현하여 빈으로 등록합니다.
 */
interface ResourceTypeRegistry {
    /** 리소스 타입 이름을 검증하고 정규화된 이름을 반환합니다. 유효하지 않으면 예외를 던집니다. */
    fun resolve(name: String): String

    /** 등록된 모든 리소스 타입 정보를 반환합니다. */
    fun allEntries(): List<ResourceTypeInfo>
}

data class ResourceTypeInfo(
    val key: String,
    val resourceName: String,
    val endpoint: String,
)

/**
 * ResourceTypeRegistry가 등록되지 않은 경우 사용되는 기본 구현.
 */
class DefaultResourceTypeRegistry : ResourceTypeRegistry {
    override fun resolve(name: String): String = throw CustomException(ErrorCode.INVALID_RESOURCE_TYPE, name)

    override fun allEntries(): List<ResourceTypeInfo> = emptyList()
}
