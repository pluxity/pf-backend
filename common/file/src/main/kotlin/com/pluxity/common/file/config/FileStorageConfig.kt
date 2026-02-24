package com.pluxity.common.file.config

import com.pluxity.common.file.properties.FileProperties
import com.pluxity.common.file.properties.S3Properties
import com.pluxity.common.file.repository.FileRepository
import com.pluxity.common.file.repository.ZipContentEntryRepository
import com.pluxity.common.file.service.FileService
import com.pluxity.common.file.strategy.storage.LocalStorageStrategy
import com.pluxity.common.file.strategy.storage.S3StorageStrategy
import com.pluxity.common.file.strategy.storage.StorageStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@EnableConfigurationProperties(FileProperties::class, S3Properties::class)
class FileStorageConfig {
    @Bean
    fun fileService(
        storageStrategy: StorageStrategy,
        fileRepository: FileRepository,
        zipContentEntryRepository: ZipContentEntryRepository,
        s3Properties: S3Properties,
        s3Presigner: S3Presigner,
        fileProperties: FileProperties,
    ): FileService = FileService(s3Presigner, s3Properties, storageStrategy, fileRepository, zipContentEntryRepository, fileProperties)

    @Bean
    @ConditionalOnProperty(name = ["file.storage-strategy"], havingValue = "local")
    fun localStorageStrategy(fileProperties: FileProperties): StorageStrategy = LocalStorageStrategy(fileProperties)

    @Bean
    @ConditionalOnProperty(name = ["file.storage-strategy"], havingValue = "s3")
    fun s3StorageStrategy(
        s3Properties: S3Properties,
        s3Client: S3Client,
    ): StorageStrategy = S3StorageStrategy(s3Properties, s3Client)
}
