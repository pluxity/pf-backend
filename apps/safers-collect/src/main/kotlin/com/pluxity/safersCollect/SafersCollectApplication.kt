package com.pluxity.safersCollect

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
class SafersCollectApplication

fun main(args: Array<String>) {
    runApplication<SafersCollectApplication>(*args)
}
