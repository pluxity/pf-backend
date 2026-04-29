package com.pluxity.safersCollect.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun safersCollectOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Safers Collect API")
                .description("현장 수집 모듈 — 센서별 데이터/이벤트/디바이스 CRUD (인증 없음, 사이트 LAN 격리)")
                .version("v0.1"),
        )

    // Swagger UI 우상단 드롭다운에서 v1 / v2 / ... 버전을 선택해 볼 수 있도록 그룹 분리.
    @Bean
    fun v1Group(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("v1")
            .pathsToMatch("/v1/**")
            .packagesToScan("com.pluxity.safersCollect.v1")
            .build()
}
