package com.pluxity.weekly.global.config

import com.pluxity.common.auth.config.SecurityPermitConfigurer
import org.springframework.context.annotation.Configuration

@Configuration
class WeeklyReportPermitConfigurer: SecurityPermitConfigurer {
    override fun permitPaths(): List<String> =
        listOf()
}