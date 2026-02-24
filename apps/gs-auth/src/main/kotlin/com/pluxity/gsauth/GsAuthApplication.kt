package com.pluxity.gsauth

import com.pluxity.common.core.annotation.PlatformApplication
import org.springframework.boot.runApplication

@PlatformApplication
class GsAuthApplication

fun main(args: Array<String>) {
    runApplication<GsAuthApplication>(*args)
}
