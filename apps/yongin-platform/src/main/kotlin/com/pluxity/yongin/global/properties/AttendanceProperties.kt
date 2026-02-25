package com.pluxity.yongin.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "attendance")
data class AttendanceProperties
    @ConstructorBinding
    constructor(
        val url: String,
    )
