package com.pluxity.yongin.global.config

import com.pluxity.common.core.config.ApiConfigurer
import com.pluxity.common.core.config.apiGroup
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YonginApiConfigurer : ApiConfigurer {
    override fun openApiInfo(): Info =
        Info()
            .title("Yongin Platform API")
            .description("Yongin Platform API Documentation")
            .version("1.0.0")

    @Bean
    fun processStatusApi(): GroupedOpenApi = apiGroup("5. 공정현황 관리 API", "/process-statuses/**")

    @Bean
    fun goalApi(): GroupedOpenApi = apiGroup("6. 목표 관리 API", "/goals/**")

    @Bean
    fun keyManagementApi(): GroupedOpenApi = apiGroup("7. 주요관리사항 관리 API", "/key-management/**")

    @Bean
    fun attendanceApi(): GroupedOpenApi = apiGroup("8. 출역현황 관리 API", "/attendances/**")
}
