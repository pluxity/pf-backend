package com.pluxity.common.core.config

import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi

/**
 * 앱별 Swagger(OpenAPI) 설정을 위한 인터페이스.
 * 각 앱에서 @Configuration으로 구현하고, @Bean으로 API 그룹을 직접 등록합니다.
 */
interface ApiConfigurer {
    /** 앱 고유 OpenAPI 정보 (title, description, version 등) */
    fun openApiInfo(): Info
}

/** GroupedOpenApi를 간결하게 생성하는 헬퍼 */
fun apiGroup(
    group: String,
    vararg pathsToMatch: String,
): GroupedOpenApi =
    GroupedOpenApi
        .builder()
        .group(group)
        .pathsToMatch(*pathsToMatch)
        .build()
