package com.pluxity.common.core.annotation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConfigurationPropertiesScan
@EntityScan(basePackages = ["com.pluxity"])
@EnableJpaRepositories(basePackages = ["com.pluxity"])
@SpringBootApplication(scanBasePackages = ["com.pluxity"])
annotation class PlatformApplication
