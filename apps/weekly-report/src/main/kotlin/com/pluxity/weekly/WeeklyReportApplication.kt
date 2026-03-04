package com.pluxity.weekly

import com.pluxity.common.core.annotation.PlatformApplication
import org.springframework.boot.runApplication

@PlatformApplication
class WeeklyReportApplication

fun main(args: Array<String>) {
    runApplication<WeeklyReportApplication>(*args)
}
