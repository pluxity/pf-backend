package com.pluxity.safers.global.config

import com.pluxity.common.core.config.ApiConfigurer
import com.pluxity.common.core.config.apiGroup
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SafersApiConfigurer : ApiConfigurer {
    override fun openApiInfo(): Info =
        Info()
            .title("Safers API")
            .description("Safers Platform API Documentation")
            .version("1.0.0")

    @Bean
    fun siteApi(): GroupedOpenApi = apiGroup("5. 현장", "/sites/**")

    @Bean
    fun eventApi(): GroupedOpenApi = apiGroup("6. 이벤트", "/events/**")

    @Bean
    fun cctvApi(): GroupedOpenApi = apiGroup("7. CCTV", "/cctvs/**")

    @Bean
    fun collectApi(): GroupedOpenApi = apiGroup("8. 수집", "/collect/**")
}
