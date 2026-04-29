package com.pluxity.safers.global.config

import com.pluxity.common.auth.config.SecurityPermitConfigurer
import org.springframework.context.annotation.Configuration

@Configuration
class SafersSecurityPermitConfigurer : SecurityPermitConfigurer {
    override fun permitPaths(): List<String> =
        listOf(
            "/events/**",
            "/collect/**",
            // 수집모듈 → 중앙 ingest. 인증은 추후 X-Api-Key Filter 로 처리 예정 (§2.4 / §13).
            "/v1/sites/**",
        )
}
