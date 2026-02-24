package com.pluxity.common.core.config

import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

/**
 * 앱에서 ApiConfigurer를 구현하지 않은 경우 사용되는 기본 구현체.
 */
@Component
@ConditionalOnMissingBean(ApiConfigurer::class)
class DefaultApiConfigurer : ApiConfigurer {
    override fun openApiInfo(): Info =
        Info()
            .title("Pluxity Platform API")
            .description("Pluxity Platform API Documentation")
            .version("1.0.0")
}
