package com.pluxity.yongin

import com.pluxity.common.core.annotation.PlatformApplication
import org.springframework.boot.runApplication

@PlatformApplication
class YonginPlatformApplication

fun main(args: Array<String>) {
    runApplication<YonginPlatformApplication>(*args)
}
