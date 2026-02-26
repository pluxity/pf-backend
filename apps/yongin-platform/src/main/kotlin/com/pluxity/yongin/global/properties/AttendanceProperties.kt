package com.pluxity.yongin.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "attendance")
data class AttendanceProperties(
    val url: String,
)
