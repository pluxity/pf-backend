package com.pluxity.safersCollect.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        // 버전별 패키지에 자동으로 prefix 부여. 새 버전 추가 시 한 줄만 더하면 끝.
        configurer.addPathPrefix("/v1") {
            it.isAnnotationPresent(RestController::class.java) &&
                it.packageName.startsWith("com.pluxity.safersCollect.v1")
        }
        // 향후:
        // configurer.addPathPrefix("/v2") {
        //     it.isAnnotationPresent(RestController::class.java) &&
        //         it.packageName.startsWith("com.pluxity.safersCollect.v2")
        // }
    }
}
