package com.pluxity.safers.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "event")
data class EventProperties
    @ConstructorBinding
    constructor(
        val baseUrl: String,
    )
