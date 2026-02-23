package com.pluxity.yonginplatform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.pluxity"])
class YonginPlatformApplication

fun main(args: Array<String>) {
    runApplication<YonginPlatformApplication>(*args)
}
