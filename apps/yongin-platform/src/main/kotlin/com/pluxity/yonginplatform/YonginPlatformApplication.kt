package com.pluxity.yonginplatform

import com.pluxity.common.core.annotation.PlatformApplication
import org.springframework.boot.runApplication

@PlatformApplication
class YonginPlatformApplication

fun main(args: Array<String>) {
    runApplication<YonginPlatformApplication>(*args)
}
