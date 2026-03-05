package com.pluxity.weekly.global.config

import com.pluxity.common.core.config.ApiConfigurer
import com.pluxity.common.core.config.apiGroup
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeeklyReportApiConfigurer : ApiConfigurer {
    override fun openApiInfo(): Info =
        Info()
            .title("Weekly Report API")
            .description("Weekly Report Platform API Documentation")
            .version("1.0.0")

    @Bean
    fun teamApi(): GroupedOpenApi = apiGroup("5. 팀 관리 API", "/teams/**")

    @Bean
    fun projectApi(): GroupedOpenApi = apiGroup("6. 프로젝트 관리 API", "/projects/**")
}
