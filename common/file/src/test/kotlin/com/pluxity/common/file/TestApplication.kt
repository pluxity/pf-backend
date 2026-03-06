package com.pluxity.common.file

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication(
    scanBasePackages = [
        "com.pluxity.common.core.exception",
        "com.pluxity.common.file.controller",
    ],
)
@EnableWebSecurity
class TestApplication
