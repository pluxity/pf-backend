package com.pluxity.safers.global.config

import com.pluxity.common.auth.config.SecurityPermitConfigurer
import org.springframework.context.annotation.Configuration

@Configuration
class SafersSecurityPermitConfigurer : SecurityPermitConfigurer {
    override fun permitPaths(): List<String> =
        listOf(
            "/events/**",
        )
}
