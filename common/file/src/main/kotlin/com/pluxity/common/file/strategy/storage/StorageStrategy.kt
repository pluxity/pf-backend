package com.pluxity.common.file.strategy.storage

interface StorageStrategy {
    fun save(context: FileProcessingContext): String

    fun persist(context: FilePersistenceContext): String
}
