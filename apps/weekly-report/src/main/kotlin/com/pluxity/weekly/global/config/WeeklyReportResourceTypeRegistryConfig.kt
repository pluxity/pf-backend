package com.pluxity.weekly.global.config

import com.pluxity.common.auth.permission.DefaultResourceTypeRegistry
import com.pluxity.common.auth.permission.ResourceTypeRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeeklyReportResourceTypeRegistryConfig {
    @Bean
    fun resourceTypeRegistry(): ResourceTypeRegistry = DefaultResourceTypeRegistry()
}
