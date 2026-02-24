package com.pluxity.yonginplatform.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "media-server")
data class MediaServerProperties
    @ConstructorBinding
    constructor(
        val url: String,
    )
