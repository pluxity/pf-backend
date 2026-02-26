package com.pluxity.yongin.global.config

import com.pluxity.common.auth.config.SecurityPermitConfigurer
import org.springframework.context.annotation.Configuration

@Configuration
class YonginSecurityPermitConfigurer : SecurityPermitConfigurer {
    override fun permitPaths(): List<String> =
        listOf(
            "/weather/webhook",
            "/users/usernames",
            "/cctv-bookmarks/*",
        )
}
