package com.pluxity.common.core.annotation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["com.pluxity"])
annotation class PlatformApplication
