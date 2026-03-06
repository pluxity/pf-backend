package com.pluxity.common.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication(
    scanBasePackages = [
        "com.pluxity.common.core.exception",
        "com.pluxity.common.auth.user.controller",
        "com.pluxity.common.auth.authentication.controller",
        "com.pluxity.common.auth.permission",
    ],
)
@EnableWebSecurity
class TestApplication
