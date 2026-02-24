package com.pluxity.common.core.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.License
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonApiConfig(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val apiConfigurer: ApiConfigurer,
) {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                apiConfigurer
                    .openApiInfo()
                    .contact(Contact().name("Pluxity").email("support@pluxity.com"))
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("http://www.apache.org/licenses/LICENSE-2.0.html"),
                    ),
            )

    @Bean
    fun commonApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("1. 전체")
            .pathsToMatch("/**")
            .build()

    @Bean
    fun authApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("2. 인증")
            .pathsToMatch("/auth/**")
            .pathsToExclude("/users/**", "/admin/**", "/other/**")
            .build()

    @Bean
    fun fileApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("3. 파일관리 API")
            .pathsToMatch("/files/**")
            .build()

    @Bean
    fun userApiByPath(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("4. 사용자 API")
            .pathsToMatch("/users/**", "/admin/users/**", "/roles/**", "/permissions/**")
            .build()
}
