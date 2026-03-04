package com.pluxity.weekly.global.config

import com.pluxity.common.core.config.ApiConfigurer
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
class WeeklyReportApiConfigurer : ApiConfigurer {
    override fun openApiInfo(): Info =
        Info()
            .title("Weekly Report API")
            .description("Weekly Report Platform API Documentation")
            .version("1.0.0")
}
