package com.pluxity.safers

import com.pluxity.common.core.annotation.PlatformApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@PlatformApplication
class SafersApplication

fun main(args: Array<String>) {
    runApplication<SafersApplication>(*args)
}
