package com.pluxity.common.file.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "file")
data class FileProperties(
    val storageStrategy: String,
    val local: LocalProperties,
)

data class LocalProperties(
    val path: String,
)

@ConfigurationProperties(prefix = "file.s3")
data class S3Properties(
    val bucket: String,
    val region: String,
    val endpointUrl: String,
    val publicUrl: String,
    val accessKey: String,
    val secretKey: String,
    val preSignedUrlExpiration: Int = 0,
)
